package com.invoiceflow.infrastructure.persistence;

import com.invoiceflow.domain.model.DiscountType;
import com.invoiceflow.domain.model.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class InvoiceJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "public_token", unique = true)
    private String publicToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;

    @Column(name = "discount_value")
    private BigDecimal discountValue;

    @Column(name = "tax_rate")
    private BigDecimal taxRate;

    @Column(nullable = false)
    private BigDecimal total;

    private String notes;

    @Column(name = "payment_terms")
    private String paymentTerms;

    @Column(name = "chapa_payment_link")
    private String chapaPaymentLink;
 
    @Column(name = "chapa_checkout_session_id")
    private String chapaCheckoutSessionId;
 
    @Column(name = "chapa_tx_ref")
    private String chapaTxRef;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "last_reminder_sent_at")
    private LocalDateTime lastReminderSentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private int version;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "invoice_id", referencedColumnName = "id")
    @OrderBy("position ASC")
    private List<InvoiceItemJpaEntity> items = new ArrayList<>();
}
