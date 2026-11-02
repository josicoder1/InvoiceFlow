package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Proposal;

import java.util.UUID;

public interface SendProposalUseCase {
    Proposal sendProposal(Command command);

    record Command(UUID proposalId, UUID userId) {}
}
