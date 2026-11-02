package com.invoiceflow.domain.port.in;

import java.util.UUID;

public interface DeleteProposalUseCase {
    void deleteProposal(Command command);

    record Command(UUID proposalId, UUID userId) {}
}
