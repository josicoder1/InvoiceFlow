package com.invoiceflow.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "proposal_items")
@Getter
@Setter
public class ProposalItemJpaEntity {

    @Id
    private UUID id;

    @Column(name = "proposal_id")
    private UUID proposalId;

    @Column(nullable = false)
    private String description;

    private BigDecimal quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    private BigDecimal total;

    @Column(nullable = false)
    private int position;
}
