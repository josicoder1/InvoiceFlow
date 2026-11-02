package com.invoiceflow.infrastructure.persistence;

import com.invoiceflow.domain.model.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProposalJpaRepository extends JpaRepository<ProposalJpaEntity, UUID> {

    Optional<ProposalJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    Optional<ProposalJpaEntity> findByPublicToken(String publicToken);

    @Query("SELECT p FROM ProposalJpaEntity p LEFT JOIN ClientJpaEntity c ON p.clientId = c.id " +
           "WHERE p.userId = :userId " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:clientId IS NULL OR p.clientId = :clientId) " +
           "AND (:query IS NULL OR :query = '' OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<ProposalJpaEntity> findByUserIdAndFilters(
            @Param("userId") UUID userId,
            @Param("status") ProposalStatus status,
            @Param("clientId") UUID clientId,
            @Param("query") String query,
            Pageable pageable
    );

    @Query("SELECT MAX(p.proposalNumber) FROM ProposalJpaEntity p WHERE p.userId = :userId")
    Optional<String> findMaxProposalNumberByUserId(@Param("userId") UUID userId);

    long countByUserIdAndStatusIn(UUID userId, Collection<ProposalStatus> statuses);

    List<ProposalJpaEntity> findTop20ByUserIdOrderByUpdatedAtDesc(UUID userId);
}
