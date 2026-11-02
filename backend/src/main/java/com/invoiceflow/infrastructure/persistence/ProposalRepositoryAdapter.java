package com.invoiceflow.infrastructure.persistence;

import com.invoiceflow.domain.model.Proposal;
import com.invoiceflow.domain.model.ProposalItem;
import com.invoiceflow.domain.model.ProposalStatus;
import com.invoiceflow.domain.port.out.ProposalRepositoryPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ProposalRepositoryAdapter implements ProposalRepositoryPort {

    private final ProposalJpaRepository proposalJpaRepository;
    private final ProposalItemJpaRepository proposalItemJpaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public ProposalRepositoryAdapter(ProposalJpaRepository proposalJpaRepository,
                                     ProposalItemJpaRepository proposalItemJpaRepository) {
        this.proposalJpaRepository = proposalJpaRepository;
        this.proposalItemJpaRepository = proposalItemJpaRepository;
    }

    @Override
    public Proposal save(Proposal proposal) {
        boolean isNew = !proposalJpaRepository.existsById(proposal.getId());

        if (!isNew) {
            proposalItemJpaRepository.deleteAllByProposalId(proposal.getId());
            proposalItemJpaRepository.flush();
        }

        ProposalJpaEntity entity = mapToEntity(proposal);
        entity.setItems(new ArrayList<>());
        ProposalJpaEntity saved = proposalJpaRepository.saveAndFlush(entity);

        List<ProposalItemJpaEntity> itemEntities = buildItemEntities(proposal, saved.getId());
        proposalItemJpaRepository.saveAll(itemEntities);
        proposalItemJpaRepository.flush();

        entityManager.refresh(saved);
        return proposalJpaRepository.findById(saved.getId())
                .map(this::toDomain)
                .orElseThrow(() -> new IllegalStateException("Proposal not found after save"));
    }

    @Override
    public Optional<Proposal> findById(UUID id) {
        return proposalJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Proposal> findByIdAndUserId(UUID id, UUID userId) {
        return proposalJpaRepository.findByIdAndUserId(id, userId).map(this::toDomain);
    }

    @Override
    public Optional<Proposal> findByPublicToken(String publicToken) {
        return proposalJpaRepository.findByPublicToken(publicToken).map(this::toDomain);
    }

    @Override
    public void delete(Proposal proposal) {
        proposalItemJpaRepository.deleteAllByProposalId(proposal.getId());
        proposalItemJpaRepository.flush();
        proposalJpaRepository.deleteById(proposal.getId());
    }

    @Override
    public Page<Proposal> findByUserIdAndFilters(UUID userId, ProposalStatus status,
                                                  UUID clientId, String query, Pageable pageable) {
        return proposalJpaRepository.findByUserIdAndFilters(userId, status, clientId, query, pageable)
                .map(this::toDomain);
    }

    @Override
    public Optional<String> findMaxProposalNumberByUserId(UUID userId) {
        return proposalJpaRepository.findMaxProposalNumberByUserId(userId);
    }

    @Override
    public long countByUserIdAndStatusIn(UUID userId, Collection<ProposalStatus> statuses) {
        return proposalJpaRepository.countByUserIdAndStatusIn(userId, statuses);
    }

    @Override
    public List<Proposal> findRecentByUserId(UUID userId, int limit) {
        return proposalJpaRepository.findTop20ByUserIdOrderByUpdatedAtDesc(userId).stream()
                .limit(limit)
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private ProposalJpaEntity mapToEntity(Proposal p) {
        ProposalJpaEntity e = new ProposalJpaEntity();
        e.setId(p.getId());
        e.setUserId(p.getUserId());
        e.setClientId(p.getClientId());
        e.setProposalNumber(p.getProposalNumber());
        e.setTitle(p.getTitle());
        e.setPublicToken(p.getPublicToken());
        e.setStatus(p.getStatus());
        e.setOverview(p.getOverview());
        e.setScope(p.getScope());
        e.setTimeline(p.getTimeline());
        e.setPricingNotes(p.getPricingNotes());
        e.setSubtotal(p.getSubtotal());
        e.setDiscountType(p.getDiscountType());
        e.setDiscountValue(p.getDiscountValue());
        e.setTaxRate(p.getTaxRate());
        e.setTotal(p.getTotal());
        e.setValidUntil(p.getValidUntil());
        e.setSignatureData(p.getSignatureData());
        e.setSignedAt(p.getSignedAt());
        e.setViewedAt(p.getViewedAt());
        e.setConvertedToInvoiceId(p.getConvertedToInvoiceId());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        e.setVersion(p.getVersion());
        e.setItems(new ArrayList<>());
        return e;
    }

    private List<ProposalItemJpaEntity> buildItemEntities(Proposal proposal, UUID proposalId) {
        if (proposal.getItems() == null) return Collections.emptyList();
        return proposal.getItems().stream().map(item -> {
            ProposalItemJpaEntity e = new ProposalItemJpaEntity();
            e.setId(item.getId());
            e.setProposalId(proposalId);
            e.setDescription(item.getDescription());
            e.setQuantity(item.getQuantity());
            e.setUnitPrice(item.getUnitPrice());
            e.setTotal(item.getTotal());
            e.setPosition(item.getPosition());
            return e;
        }).collect(Collectors.toList());
    }

    private Proposal toDomain(ProposalJpaEntity e) {
        List<ProposalItem> items = new ArrayList<>();
        if (e.getItems() != null) {
            items = e.getItems().stream()
                    .map(i -> ProposalItem.reconstitute(
                            i.getId(), i.getProposalId(), i.getDescription(),
                            i.getQuantity(), i.getUnitPrice(), i.getTotal(), i.getPosition()))
                    .collect(Collectors.toList());
        }
        return Proposal.reconstitute(
                e.getId(), e.getUserId(), e.getClientId(), e.getProposalNumber(), e.getTitle(),
                e.getPublicToken(), e.getStatus(), e.getOverview(), e.getScope(), e.getTimeline(),
                e.getPricingNotes(), e.getSubtotal(), e.getDiscountType(), e.getDiscountValue(),
                e.getTaxRate(), e.getTotal(), e.getValidUntil(), e.getSignatureData(), e.getSignedAt(),
                e.getViewedAt(), e.getConvertedToInvoiceId(), e.getCreatedAt(), e.getUpdatedAt(),
                e.getVersion(), items);
    }
}
