package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Proposal;

import java.util.UUID;

public interface GetProposalUseCase {
    Proposal getProposal(Query query);

    record Query(UUID proposalId, UUID userId) {}
}
