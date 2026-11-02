package com.invoiceflow.infrastructure.persistence;

import com.invoiceflow.domain.model.DiscountType;
import com.invoiceflow.domain.model.ProposalStatus;
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
@Table(name = "proposals")
@Getter
@Setter
public class ProposalJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "proposal_number")
    private String proposalNumber;

    @Column(nullable = false)
    private String title;

    @Column(name = "public_token", unique = true)
    private String publicToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProposalStatus status;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(columnDefinition = "TEXT")
    private String scope;

    @Column(columnDefinition = "TEXT")
    private String timeline;

    @Column(name = "pricing_notes", columnDefinition = "TEXT")
    private String pricingNotes;

    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 15, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal total;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;

    @Column(name = "converted_to_invoice_id")
    private UUID convertedToInvoiceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private int version;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "proposal_id", referencedColumnName = "id")
    @OrderBy("position ASC")
    private List<ProposalItemJpaEntity> items = new ArrayList<>();
}
