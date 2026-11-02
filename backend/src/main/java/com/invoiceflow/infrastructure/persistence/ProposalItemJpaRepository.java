package com.invoiceflow.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProposalItemJpaRepository extends JpaRepository<ProposalItemJpaEntity, UUID> {

    @Modifying
    @Query("DELETE FROM ProposalItemJpaEntity i WHERE i.proposalId = :proposalId")
    void deleteAllByProposalId(@Param("proposalId") UUID proposalId);
}
