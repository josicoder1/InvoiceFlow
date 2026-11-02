package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Proposal;

public interface AcceptProposalUseCase {
    Proposal acceptProposal(Command command);

    record Command(String publicToken, String signatureData) {}
}
