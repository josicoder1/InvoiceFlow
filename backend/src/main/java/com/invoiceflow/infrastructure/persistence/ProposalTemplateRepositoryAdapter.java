package com.invoiceflow.infrastructure.persistence;

import com.invoiceflow.domain.port.out.ProposalTemplateRepositoryPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ProposalTemplateRepositoryAdapter implements ProposalTemplateRepositoryPort {

    private final ProposalTemplateJpaRepository templateJpaRepository;

    public ProposalTemplateRepositoryAdapter(ProposalTemplateJpaRepository templateJpaRepository) {
        this.templateJpaRepository = templateJpaRepository;
    }

    @Override
    public List<TemplateData> findAllTemplates() {
        return templateJpaRepository.findAll().stream()
                .map(t -> new TemplateData(
                        t.getId().toString(), t.getName(), t.getOverview(),
                        t.getScope(), t.getTimeline(), t.getPricingNotes()))
                .collect(Collectors.toList());
    }
}
