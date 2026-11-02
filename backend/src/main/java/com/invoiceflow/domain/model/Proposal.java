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
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Proposal {
    private UUID id;
    private UUID userId;
    private UUID clientId;
    private String proposalNumber;
    private String title;
    private String publicToken;
    private ProposalStatus status;
    private String overview;
    private String scope;
    private String timeline;
    private String pricingNotes;
    private BigDecimal subtotal;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal taxRate;
    private BigDecimal total;
    private LocalDate validUntil;
    private String signatureData;
    private LocalDateTime signedAt;
    private LocalDateTime viewedAt;
    private UUID convertedToInvoiceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;
    private List<ProposalItem> items;

    public static Proposal create(
            UUID userId, UUID clientId, String proposalNumber, String title,
            String overview, String scope, String timeline, String pricingNotes,
            LocalDate validUntil, List<ProposalItem> items,
            DiscountType discountType, BigDecimal discountValue, BigDecimal taxRate) {

        if (userId == null) throw new IllegalArgumentException("User ID cannot be null");
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (proposalNumber == null || proposalNumber.isBlank()) {
            throw new IllegalArgumentException("Proposal number cannot be empty");
        }
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title cannot be empty");

        UUID proposalId = UUID.randomUUID();
        List<ProposalItem> assignedItems = new ArrayList<>();
        if (items != null) {
            for (ProposalItem item : items) {
                item.assignProposal(proposalId);
                assignedItems.add(item);
            }
        }

        Proposal proposal = new Proposal(
                proposalId,
                userId,
                clientId,
                proposalNumber,
                title,
                UUID.randomUUID().toString(),
                ProposalStatus.DRAFT,
                overview,
                scope,
                timeline,
                pricingNotes,
                BigDecimal.ZERO,
                discountType,
                discountValue != null ? discountValue : BigDecimal.ZERO,
                taxRate != null ? taxRate : BigDecimal.ZERO,
                BigDecimal.ZERO,
                validUntil,
                null,
                null,
                null,
                null,
                LocalDateTime.now(),
                LocalDateTime.now(),
                0,
                assignedItems
        );
        proposal.calculateTotals();
        return proposal;
    }

    public static Proposal reconstitute(
            UUID id, UUID userId, UUID clientId, String proposalNumber, String title, String publicToken,
            ProposalStatus status, String overview, String scope, String timeline, String pricingNotes,
            BigDecimal subtotal, DiscountType discountType, BigDecimal discountValue, BigDecimal taxRate,
            BigDecimal total, LocalDate validUntil, String signatureData, LocalDateTime signedAt,
            LocalDateTime viewedAt, UUID convertedToInvoiceId, LocalDateTime createdAt,
            LocalDateTime updatedAt, int version, List<ProposalItem> items) {
        return new Proposal(id, userId, clientId, proposalNumber, title, publicToken, status, overview, scope,
                timeline, pricingNotes, subtotal, discountType, discountValue, taxRate, total, validUntil,
                signatureData, signedAt, viewedAt, convertedToInvoiceId, createdAt, updatedAt, version,
                items != null ? items : new ArrayList<>());
    }

    public void send() {
        if (this.status != ProposalStatus.DRAFT) {
            throw new BusinessRuleViolationException("Only DRAFT proposals can be sent");
        }
        if (this.items == null || this.items.isEmpty()) {
            throw new BusinessRuleViolationException("Cannot send a proposal with no items");
        }
        this.status = ProposalStatus.SENT;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean recordViewed() {
        if (this.viewedAt != null || this.status == ProposalStatus.VIEWED) {
            return false;
        }
        if (this.status == ProposalStatus.ACCEPTED || this.status == ProposalStatus.DECLINED) {
            return false;
        }
        if (this.status != ProposalStatus.SENT) {
            throw new BusinessRuleViolationException("Proposal must be in SENT status to record a view");
        }
        this.status = ProposalStatus.VIEWED;
        this.viewedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        return true;
    }

    public void accept(String signatureData) {
        if (this.status != ProposalStatus.SENT && this.status != ProposalStatus.VIEWED) {
            throw new BusinessRuleViolationException("Proposal must be SENT or VIEWED to be accepted");
        }
        if (isExpired()) {
            throw new BusinessRuleViolationException("Cannot accept an expired proposal");
        }
        if (signatureData == null || signatureData.isBlank()) {
            throw new BusinessRuleViolationException("Signature is required to accept a proposal");
        }
        this.status = ProposalStatus.ACCEPTED;
        this.signatureData = signatureData;
        this.signedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void decline() {
        if (this.status != ProposalStatus.SENT && this.status != ProposalStatus.VIEWED) {
            throw new BusinessRuleViolationException("Proposal must be SENT or VIEWED to be declined");
        }
        this.status = ProposalStatus.DECLINED;
        this.updatedAt = LocalDateTime.now();
    }

    public void edit(UUID clientId, String title, String overview, String scope,
                     String timeline, String pricingNotes, LocalDate validUntil,
                     List<ProposalItem> items, DiscountType discountType,
                     BigDecimal discountValue, BigDecimal taxRate) {
        if (this.status != ProposalStatus.DRAFT) {
            throw new BusinessRuleViolationException("Can only edit proposals in DRAFT status");
        }
        if (clientId == null) throw new IllegalArgumentException("Client ID cannot be null");
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title cannot be empty");

        this.clientId = clientId;
        this.title = title;
        this.overview = overview;
        this.scope = scope;
        this.timeline = timeline;
        this.pricingNotes = pricingNotes;
        this.validUntil = validUntil;
        this.discountType = discountType;
        this.discountValue = discountValue != null ? discountValue : BigDecimal.ZERO;
        this.taxRate = taxRate != null ? taxRate : BigDecimal.ZERO;

        this.items.clear();
        if (items != null) {
            for (ProposalItem item : items) {
                item.assignProposal(this.id);
                this.items.add(item);
            }
        }

        this.calculateTotals();
        this.updatedAt = LocalDateTime.now();
    }

    public void markConverted(UUID invoiceId) {
        if (this.status != ProposalStatus.ACCEPTED) {
            throw new BusinessRuleViolationException("Only ACCEPTED proposals can be converted to invoices");
        }
        if (this.convertedToInvoiceId != null) {
            throw new BusinessRuleViolationException("Proposal has already been converted to an invoice");
        }
        this.convertedToInvoiceId = invoiceId;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return validUntil != null && LocalDate.now().isAfter(validUntil);
    }

    public boolean isActionable() {
        return (status == ProposalStatus.SENT || status == ProposalStatus.VIEWED) && !isExpired();
    }

    private void calculateTotals() {
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        for (ProposalItem item : this.items) {
            if (item.getTotal() != null) {
                calculatedSubtotal = calculatedSubtotal.add(item.getTotal());
            }
        }
        this.subtotal = calculatedSubtotal.setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = BigDecimal.ZERO;
        if (this.discountType != null && this.discountValue != null && this.discountValue.compareTo(BigDecimal.ZERO) > 0) {
            if (this.discountType == DiscountType.PERCENT) {
                discount = this.subtotal.multiply(this.discountValue)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
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
            tax = discountedSubtotal.multiply(this.taxRate)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        this.total = discountedSubtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
    }
}
