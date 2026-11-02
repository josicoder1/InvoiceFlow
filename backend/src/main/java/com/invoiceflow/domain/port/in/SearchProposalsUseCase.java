package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Proposal;
import com.invoiceflow.domain.model.ProposalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SearchProposalsUseCase {
    Page<Proposal> searchProposals(Query query);

    record Query(UUID userId, ProposalStatus status, UUID clientId, String searchQuery, Pageable pageable) {}
}
