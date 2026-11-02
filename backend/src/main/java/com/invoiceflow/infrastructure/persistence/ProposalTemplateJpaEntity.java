package com.invoiceflow.infrastructure.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "proposal_templates")
@Getter
@Setter
public class ProposalTemplateJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String overview;

    @Column(columnDefinition = "TEXT")
    private String scope;

    @Column(columnDefinition = "TEXT")
    private String timeline;

    @Column(name = "pricing_notes", columnDefinition = "TEXT")
    private String pricingNotes;
}
