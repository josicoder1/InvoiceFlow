package com.invoiceflow.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProposalTemplateJpaRepository extends JpaRepository<ProposalTemplateJpaEntity, UUID> {
}
