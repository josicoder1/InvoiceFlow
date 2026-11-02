package com.invoiceflow.infrastructure.web;

import com.invoiceflow.domain.port.in.GetProposalTemplatesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/proposal-templates")
public class ProposalTemplateController {

    private final GetProposalTemplatesUseCase getProposalTemplatesUseCase;

    public ProposalTemplateController(GetProposalTemplatesUseCase getProposalTemplatesUseCase) {
        this.getProposalTemplatesUseCase = getProposalTemplatesUseCase;
    }

    @GetMapping
    public ResponseEntity<List<GetProposalTemplatesUseCase.ProposalTemplate>> getTemplates() {
        return ResponseEntity.ok(getProposalTemplatesUseCase.getTemplates());
    }
}
