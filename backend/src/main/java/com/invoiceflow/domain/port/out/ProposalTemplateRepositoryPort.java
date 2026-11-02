package com.invoiceflow.domain.port.out;

import java.util.List;

public interface ProposalTemplateRepositoryPort {
    List<TemplateData> findAllTemplates();

    record TemplateData(String id, String name, String overview,
                        String scope, String timeline, String pricingNotes) {}
}
