package com.invoiceflow.domain.port.in;

import java.util.List;

public interface GetProposalTemplatesUseCase {
    List<ProposalTemplate> getTemplates();

    record ProposalTemplate(
            String id, String name, String overview,
            String scope, String timeline, String pricingNotes
    ) {}
}
