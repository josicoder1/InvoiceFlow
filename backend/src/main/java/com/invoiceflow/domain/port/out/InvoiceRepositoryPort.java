package com.invoiceflow.domain.port.out;

import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepositoryPort {

    record MonthlyRevenue(int year, int month, BigDecimal revenue) {}
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(UUID id);
    Optional<Invoice> findByIdAndUserId(UUID id, UUID userId);
    void delete(Invoice invoice);
    Page<Invoice> findByUserIdAndFilters(
            UUID userId, InvoiceStatus status, UUID clientId,
            LocalDate startDate, LocalDate endDate, String query, Pageable pageable);
    boolean existsActiveInvoicesForClient(UUID clientId);
    Optional<String> findMaxInvoiceNumberByUserId(UUID userId);
    List<Invoice> findSentInvoicesPastDueDate(LocalDate today);
    Optional<Invoice> findByPublicToken(String publicToken);
    List<Invoice> findInvoicesNeedingReminder(LocalDate today);
    long countByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end);
    List<Invoice> findAllByUserId(UUID userId);
    List<MonthlyRevenue> sumPaidRevenueByMonth(UUID userId, LocalDateTime start, LocalDateTime end);
    BigDecimal sumTotalByUserIdAndStatus(UUID userId, InvoiceStatus status);
    BigDecimal sumTotalByUserIdAndStatusAndDueDateBetween(UUID userId, InvoiceStatus status, LocalDate start, LocalDate end);
    List<Invoice> findRecentByUserId(UUID userId, int limit);
}
