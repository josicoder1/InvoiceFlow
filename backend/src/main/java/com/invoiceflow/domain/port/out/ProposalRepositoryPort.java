package com.invoiceflow.domain.port.out;

import com.invoiceflow.domain.model.Proposal;
import com.invoiceflow.domain.model.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProposalRepositoryPort {
    Proposal save(Proposal proposal);
    Optional<Proposal> findById(UUID id);
    Optional<Proposal> findByIdAndUserId(UUID id, UUID userId);
    Optional<Proposal> findByPublicToken(String publicToken);
    void delete(Proposal proposal);
    Page<Proposal> findByUserIdAndFilters(UUID userId, ProposalStatus status, UUID clientId, String query, Pageable pageable);
    Optional<String> findMaxProposalNumberByUserId(UUID userId);
    long countByUserIdAndStatusIn(UUID userId, Collection<ProposalStatus> statuses);
    List<Proposal> findRecentByUserId(UUID userId, int limit);
}
