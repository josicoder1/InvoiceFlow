package com.invoiceflow.domain.model;

import com.invoiceflow.domain.exception.BusinessRuleViolationException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Invoice {
    private UUID id;
    private UUID userId;
    private UUID clientId;
    private String invoiceNumber;
    private String publicToken;
    private InvoiceStatus status;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String currency;
    private BigDecimal subtotal;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal taxRate;
    private BigDecimal total;
    private String notes;
    private String paymentTerms;
    private String chapaPaymentLink;
    private String chapaCheckoutSessionId;
    private String chapaTxRef;
    private LocalDateTime paidAt;
    private LocalDateTime lastReminderSentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;
    private List<InvoiceItem> items;

    public static Invoice create(
            UUID userId, UUID clientId, String invoiceNumber, LocalDate issueDate, LocalDate dueDate,
            String currency, List<InvoiceItem> items, DiscountType discountType, BigDecimal discountValue,
            BigDecimal taxRate, String notes, String paymentTerms) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new IllegalArgumentException("Invoice number cannot be empty");
        }
        if (issueDate == null) {
            throw new IllegalArgumentException("Issue date cannot be null");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date cannot be null");
        }
        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Due date cannot be before issue date");
        }

        UUID invoiceId = UUID.randomUUID();
        List<InvoiceItem> assignedItems = new ArrayList<>();
        if (items != null) {
            for (InvoiceItem item : items) {
                item.assignInvoice(invoiceId);
                assignedItems.add(item);
            }
        }

        Invoice invoice = new Invoice(
                invoiceId,
                userId,
                clientId,
                invoiceNumber,
                null,
                InvoiceStatus.DRAFT,
                issueDate,
                dueDate,
                currency != null ? currency : "USD",
                BigDecimal.ZERO,
                discountType,
                discountValue != null ? discountValue : BigDecimal.ZERO,
                taxRate != null ? taxRate : BigDecimal.ZERO,
                BigDecimal.ZERO,
                notes,
                paymentTerms,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                0,
                assignedItems
        );
        invoice.calculateTotals();
        return invoice;
    }

    public static Invoice reconstitute(
            UUID id, UUID userId, UUID clientId, String invoiceNumber, String publicToken, InvoiceStatus status,
            LocalDate issueDate, LocalDate dueDate, String currency, BigDecimal subtotal,
            DiscountType discountType, BigDecimal discountValue, BigDecimal taxRate, BigDecimal total,
            String notes, String paymentTerms, String chapaPaymentLink,
            String chapaCheckoutSessionId, String chapaTxRef, LocalDateTime paidAt,
            LocalDateTime lastReminderSentAt,
            LocalDateTime createdAt, LocalDateTime updatedAt, int version, List<InvoiceItem> items) {
        return new Invoice(id, userId, clientId, invoiceNumber, publicToken, status, issueDate, dueDate, currency,
                subtotal, discountType, discountValue, taxRate, total, notes, paymentTerms,
                chapaPaymentLink, chapaCheckoutSessionId, chapaTxRef, paidAt, lastReminderSentAt,
                createdAt, updatedAt, version, items != null ? items : new ArrayList<>());
    }

    public boolean isPayable() {
        return this.status == InvoiceStatus.SENT || this.status == InvoiceStatus.OVERDUE;
    }

    public void ensurePublicToken() {
        if (this.publicToken == null || this.publicToken.isBlank()) {
            this.publicToken = UUID.randomUUID().toString();
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void edit(
            UUID clientId, LocalDate issueDate, LocalDate dueDate, String currency,
            List<InvoiceItem> items, DiscountType discountType, BigDecimal discountValue,
            BigDecimal taxRate, String notes, String paymentTerms) {

        if (this.status != InvoiceStatus.DRAFT) {
            throw new BusinessRuleViolationException("Can only edit invoices in DRAFT status");
        }
        if (clientId == null) {
            throw new IllegalArgumentException("Client ID cannot be null");
        }
        if (issueDate == null) {
            throw new IllegalArgumentException("Issue date cannot be null");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date cannot be null");
        }
        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Due date cannot be before issue date");
        }

        this.clientId = clientId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.currency = currency != null ? currency : "USD";
        this.discountType = discountType;
        this.discountValue = discountValue != null ? discountValue : BigDecimal.ZERO;
        this.taxRate = taxRate != null ? taxRate : BigDecimal.ZERO;
        this.notes = notes;
        this.paymentTerms = paymentTerms;

        this.items.clear();
        if (items != null) {
            for (InvoiceItem item : items) {
                item.assignInvoice(this.id);
                this.items.add(item);
            }
        }

        this.calculateTotals();
        this.updatedAt = LocalDateTime.now();
    }

    public void markSent() {
        if (this.status == InvoiceStatus.SENT) {
            return; // Idempotent
        }
        if (this.status != InvoiceStatus.DRAFT) {
            throw new BusinessRuleViolationException("Only DRAFT invoices can be sent");
        }
        this.status = InvoiceStatus.SENT;
        this.updatedAt = LocalDateTime.now();
    }

    public void markPaid(LocalDateTime paidAt) {
        if (this.status == InvoiceStatus.PAID) {
            return; // Idempotent
        }
        if (this.status != InvoiceStatus.SENT && this.status != InvoiceStatus.OVERDUE) {
            throw new BusinessRuleViolationException("Only SENT or OVERDUE invoices can be marked as PAID");
        }
        this.status = InvoiceStatus.PAID;
        this.paidAt = paidAt != null ? paidAt : LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markOverdue() {
        if (this.status == InvoiceStatus.OVERDUE) {
            return; // Idempotent
        }
        if (this.status != InvoiceStatus.SENT) {
            throw new BusinessRuleViolationException("Only SENT invoices can be marked as OVERDUE");
        }
        this.status = InvoiceStatus.OVERDUE;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == InvoiceStatus.CANCELLED) {
            return; // Idempotent
        }
        if (this.status == InvoiceStatus.PAID) {
            throw new BusinessRuleViolationException("Cannot cancel a PAID invoice");
        }
        this.status = InvoiceStatus.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public Invoice duplicate(String newInvoiceNumber) {
        List<InvoiceItem> duplicatedItems = new ArrayList<>();
        for (InvoiceItem item : this.items) {
            duplicatedItems.add(InvoiceItem.create(
                    item.getDescription(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getPosition()
            ));
        }

        // Default due date to original interval or +30 days
        long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(this.issueDate, this.dueDate);
        LocalDate newIssueDate = LocalDate.now();
        LocalDate newDueDate = newIssueDate.plusDays(daysDiff > 0 ? daysDiff : 30);

        return Invoice.create(
                this.userId,
                this.clientId,
                newInvoiceNumber,
                newIssueDate,
                newDueDate,
                this.currency,
                duplicatedItems,
                this.discountType,
                this.discountValue,
                this.taxRate,
                this.notes,
                this.paymentTerms
        );
    }

    public void updateChapaPaymentLink(String chapaPaymentLink) {
        this.chapaPaymentLink = chapaPaymentLink;
        this.updatedAt = LocalDateTime.now();
    }
 
    public void recordChapaCheckout(String checkoutSessionId, String txRef) {
        this.chapaCheckoutSessionId = checkoutSessionId;
        this.chapaTxRef = txRef;
        this.updatedAt = LocalDateTime.now();
    }

    public void markReminderSent() {
        this.lastReminderSentAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private void calculateTotals() {
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        for (InvoiceItem item : this.items) {
            calculatedSubtotal = calculatedSubtotal.add(item.getTotal());
        }
        this.subtotal = calculatedSubtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = BigDecimal.ZERO;
        if (this.discountType != null && this.discountValue != null && this.discountValue.compareTo(BigDecimal.ZERO) > 0) {
            if (this.discountType == DiscountType.PERCENT) {
                discount = this.subtotal.multiply(this.discountValue).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            } else if (this.discountType == DiscountType.FIXED) {
                discount = this.discountValue.setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal discountedSubtotal = this.subtotal.subtract(discount);
        if (discountedSubtotal.compareTo(BigDecimal.ZERO) < 0) {
            discountedSubtotal = BigDecimal.ZERO;
        }

        BigDecimal tax = BigDecimal.ZERO;
        if (this.taxRate != null && this.taxRate.compareTo(BigDecimal.ZERO) > 0) {
            tax = discountedSubtotal.multiply(this.taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        this.total = discountedSubtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
    }
}
