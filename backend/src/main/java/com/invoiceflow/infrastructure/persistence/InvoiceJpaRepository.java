package com.invoiceflow.infrastructure.persistence;

import com.invoiceflow.domain.model.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceJpaRepository extends JpaRepository<InvoiceJpaEntity, UUID> {

    Optional<InvoiceJpaEntity> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByClientIdAndStatusIn(UUID clientId, Collection<InvoiceStatus> statuses);

    @Query("SELECT MAX(i.invoiceNumber) FROM InvoiceJpaEntity i WHERE i.userId = :userId")
    Optional<String> findMaxInvoiceNumberByUserId(@Param("userId") UUID userId);

    @Query("SELECT i FROM InvoiceJpaEntity i WHERE i.status = 'SENT' AND i.dueDate < :today")
    List<InvoiceJpaEntity> findSentInvoicesPastDueDate(@Param("today") LocalDate today);

    Optional<InvoiceJpaEntity> findByPublicToken(String publicToken);

    @Query("""
            SELECT i FROM InvoiceJpaEntity i
            JOIN UserJpaEntity u ON i.userId = u.id
            WHERE u.remindersEnabled = true
              AND i.status IN ('SENT', 'OVERDUE')
              AND (i.lastReminderSentAt IS NULL OR CAST(i.lastReminderSentAt AS date) < :today)
            """)
    List<InvoiceJpaEntity> findReminderCandidates(@Param("today") LocalDate today);

    @Query("SELECT i FROM InvoiceJpaEntity i LEFT JOIN ClientJpaEntity c ON i.clientId = c.id WHERE i.userId = :userId AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:clientId IS NULL OR i.clientId = :clientId) AND " +
           "(cast(:startDate as date) IS NULL OR i.issueDate >= :startDate) AND " +
           "(cast(:endDate as date) IS NULL OR i.issueDate <= :endDate) AND " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<InvoiceJpaEntity> findByUserIdAndFilters(
            @Param("userId") UUID userId,
            @Param("status") InvoiceStatus status,
            @Param("clientId") UUID clientId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("query") String query,
            Pageable pageable
    );

    long countByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);

    List<InvoiceJpaEntity> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("""
            SELECT new com.invoiceflow.infrastructure.persistence.MonthlyRevenueProjection(
                CAST(EXTRACT(YEAR FROM i.paidAt) AS int),
                CAST(EXTRACT(MONTH FROM i.paidAt) AS int),
                COALESCE(SUM(i.total), 0))
            FROM InvoiceJpaEntity i
            WHERE i.userId = :userId
              AND i.status = 'PAID'
              AND i.paidAt IS NOT NULL
              AND i.paidAt >= :start
              AND i.paidAt < :end
            GROUP BY EXTRACT(YEAR FROM i.paidAt), EXTRACT(MONTH FROM i.paidAt)
            """)
    List<MonthlyRevenueProjection> sumPaidRevenueByMonth(
            @Param("userId") UUID userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(i.total), 0) FROM InvoiceJpaEntity i WHERE i.userId = :userId AND i.status = :status")
    BigDecimal sumTotalByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") InvoiceStatus status);

    @Query("""
            SELECT COALESCE(SUM(i.total), 0) FROM InvoiceJpaEntity i
            WHERE i.userId = :userId AND i.status = :status
              AND i.dueDate >= :start AND i.dueDate <= :end
            """)
    BigDecimal sumTotalByUserIdAndStatusAndDueDateBetween(
            @Param("userId") UUID userId,
            @Param("status") InvoiceStatus status,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    List<InvoiceJpaEntity> findTop20ByUserIdOrderByUpdatedAtDesc(UUID userId);
}
