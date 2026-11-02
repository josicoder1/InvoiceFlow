package com.invoiceflow.infrastructure.persistence;

import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceItem;
import com.invoiceflow.domain.model.InvoiceStatus;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
@Component
public class InvoiceRepositoryAdapter implements InvoiceRepositoryPort {

    private final InvoiceJpaRepository invoiceJpaRepository;
    private final InvoiceItemJpaRepository invoiceItemJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public InvoiceRepositoryAdapter(InvoiceJpaRepository invoiceJpaRepository,
                                    InvoiceItemJpaRepository invoiceItemJpaRepository) {
        this.invoiceJpaRepository = invoiceJpaRepository;
        this.invoiceItemJpaRepository = invoiceItemJpaRepository;
    }

    @Override
    public Invoice save(Invoice invoice) {
        boolean isNew = !invoiceJpaRepository.existsById(invoice.getId());

        // 1. For existing invoices: delete all old items first, flush to DB immediately
        if (!isNew) {
            invoiceItemJpaRepository.deleteAllByInvoiceId(invoice.getId());
            invoiceItemJpaRepository.flush();
        }

        // 2. Build the invoice entity (without items first)
        InvoiceJpaEntity entity = mapToEntity(invoice);
        entity.getItems().clear(); // ensure empty before save

        // 3. Save invoice row
        InvoiceJpaEntity savedInvoice = invoiceJpaRepository.saveAndFlush(entity);

        // 4. Save new items with proper invoice FK
        List<InvoiceItemJpaEntity> itemEntities = buildItemEntities(invoice, savedInvoice.getId());
        invoiceItemJpaRepository.saveAll(itemEntities);
        invoiceItemJpaRepository.flush();

        // 5. Reload the full entity with items from DB to return accurate state
        entityManager.refresh(savedInvoice);
        return toDomain(savedInvoice);
    }

    @Override
    public Optional<Invoice> findById(UUID id) {
        return invoiceJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Invoice> findByIdAndUserId(UUID id, UUID userId) {
        return invoiceJpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public void delete(Invoice invoice) {
        // Delete items first to avoid FK constraint issues
        invoiceItemJpaRepository.deleteAllByInvoiceId(invoice.getId());
        invoiceItemJpaRepository.flush();
        invoiceJpaRepository.deleteById(invoice.getId());
    }

    @Override
    public Page<Invoice> findByUserIdAndFilters(
            UUID userId, InvoiceStatus status, UUID clientId,
            LocalDate startDate, LocalDate endDate, String query, Pageable pageable) {
        return invoiceJpaRepository
                .findByUserIdAndFilters(userId, status, clientId, startDate, endDate, query, pageable)
                .map(this::toDomain);
    }

    @Override
    public boolean existsActiveInvoicesForClient(UUID clientId) {
        return invoiceJpaRepository.existsByClientIdAndStatusIn(clientId, Arrays.asList(
                InvoiceStatus.DRAFT,
                InvoiceStatus.SENT,
                InvoiceStatus.OVERDUE
        ));
    }

    @Override
    public Optional<String> findMaxInvoiceNumberByUserId(UUID userId) {
        return invoiceJpaRepository.findMaxInvoiceNumberByUserId(userId);
    }

    @Override
    public List<Invoice> findSentInvoicesPastDueDate(LocalDate today) {
        return invoiceJpaRepository.findSentInvoicesPastDueDate(today)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Optional<Invoice> findByPublicToken(String publicToken) {
        return invoiceJpaRepository.findByPublicToken(publicToken).map(this::toDomain);
    }

    @Override
    public List<Invoice> findInvoicesNeedingReminder(LocalDate today) {
        return invoiceJpaRepository.findReminderCandidates(today)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countByUserIdAndCreatedAtBetween(UUID userId, LocalDateTime start, LocalDateTime end) {
        return invoiceJpaRepository.countByUserIdAndCreatedAtBetween(userId, start, end);
    }

    @Override
    public List<Invoice> findAllByUserId(UUID userId) {
        return invoiceJpaRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<MonthlyRevenue> sumPaidRevenueByMonth(UUID userId, LocalDateTime start, LocalDateTime end) {
        return invoiceJpaRepository.sumPaidRevenueByMonth(userId, start, end).stream()
                .map(p -> new MonthlyRevenue(p.year(), p.month(), p.revenue()))
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal sumTotalByUserIdAndStatus(UUID userId, InvoiceStatus status) {
        return invoiceJpaRepository.sumTotalByUserIdAndStatus(userId, status);
    }

    @Override
    public BigDecimal sumTotalByUserIdAndStatusAndDueDateBetween(
            UUID userId, InvoiceStatus status, LocalDate start, LocalDate end) {
        return invoiceJpaRepository.sumTotalByUserIdAndStatusAndDueDateBetween(userId, status, start, end);
    }

    @Override
    public List<Invoice> findRecentByUserId(UUID userId, int limit) {
        return invoiceJpaRepository.findTop20ByUserIdOrderByUpdatedAtDesc(userId).stream()
                .limit(limit)
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private InvoiceJpaEntity mapToEntity(Invoice domain) {
        InvoiceJpaEntity entity = new InvoiceJpaEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setClientId(domain.getClientId());
        entity.setInvoiceNumber(domain.getInvoiceNumber());
        entity.setPublicToken(domain.getPublicToken());
        entity.setStatus(domain.getStatus());
        entity.setIssueDate(domain.getIssueDate());
        entity.setDueDate(domain.getDueDate());
        entity.setCurrency(domain.getCurrency());
        entity.setSubtotal(domain.getSubtotal());
        entity.setDiscountType(domain.getDiscountType());
        entity.setDiscountValue(domain.getDiscountValue());
        entity.setTaxRate(domain.getTaxRate());
        entity.setTotal(domain.getTotal());
        entity.setNotes(domain.getNotes());
        entity.setPaymentTerms(domain.getPaymentTerms());
        entity.setChapaPaymentLink(domain.getChapaPaymentLink());
        entity.setChapaCheckoutSessionId(domain.getChapaCheckoutSessionId());
        entity.setChapaTxRef(domain.getChapaTxRef());
        entity.setPaidAt(domain.getPaidAt());
        entity.setLastReminderSentAt(domain.getLastReminderSentAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setVersion(domain.getVersion());
        entity.setItems(new ArrayList<>());
        return entity;
    }

    private List<InvoiceItemJpaEntity> buildItemEntities(Invoice invoice, UUID invoiceId) {
        if (invoice.getItems() == null) return Collections.emptyList();
        return invoice.getItems().stream().map(item -> {
            InvoiceItemJpaEntity e = new InvoiceItemJpaEntity();
            e.setId(item.getId());
            e.setInvoiceId(invoiceId);
            e.setDescription(item.getDescription());
            e.setQuantity(item.getQuantity());
            e.setUnitPrice(item.getUnitPrice());
            e.setTotal(item.getTotal());
            e.setPosition(item.getPosition());
            return e;
        }).collect(Collectors.toList());
    }

    private Invoice toDomain(InvoiceJpaEntity entity) {
        List<InvoiceItem> domainItems = new ArrayList<>();
        if (entity.getItems() != null) {
            domainItems = entity.getItems().stream()
                    .map(i -> InvoiceItem.reconstitute(
                            i.getId(),
                            i.getInvoiceId(),
                            i.getDescription(),
                            i.getQuantity(),
                            i.getUnitPrice(),
                            i.getTotal(),
                            i.getPosition()
                    ))
                    .collect(Collectors.toList());
        }
        return Invoice.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getClientId(),
                entity.getInvoiceNumber(),
                entity.getPublicToken(),
                entity.getStatus(),
                entity.getIssueDate(),
                entity.getDueDate(),
                entity.getCurrency(),
                entity.getSubtotal(),
                entity.getDiscountType(),
                entity.getDiscountValue(),
                entity.getTaxRate(),
                entity.getTotal(),
                entity.getNotes(),
                entity.getPaymentTerms(),
                entity.getChapaPaymentLink(),
                entity.getChapaCheckoutSessionId(),
                entity.getChapaTxRef(),
                entity.getPaidAt(),
                entity.getLastReminderSentAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion(),
                domainItems
        );
    }
}
