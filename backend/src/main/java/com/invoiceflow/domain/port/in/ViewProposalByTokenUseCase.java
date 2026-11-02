package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Proposal;

public interface ViewProposalByTokenUseCase {
    Proposal viewByToken(String publicToken);
}
