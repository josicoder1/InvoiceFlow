package com.invoiceflow.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ClientJpaRepository extends JpaRepository<ClientJpaEntity, UUID> {

    Optional<ClientJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndEmail(UUID userId, String email);

    boolean existsByUserIdAndEmailAndIdNot(UUID userId, String email, UUID id);

    @Query("SELECT c FROM ClientJpaEntity c WHERE c.userId = :userId AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.company) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<ClientJpaEntity> findByUserIdAndSearch(
            @Param("userId") UUID userId,
            @Param("query") String query,
            Pageable pageable
    );

    long countByUserId(UUID userId);
}
