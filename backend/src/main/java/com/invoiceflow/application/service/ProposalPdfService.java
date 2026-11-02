package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Proposal;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.DownloadProposalPdfUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.PdfGeneratorPort;
import com.invoiceflow.domain.port.out.ProposalRepositoryPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProposalPdfService implements DownloadProposalPdfUseCase {

    private final ProposalRepositoryPort proposalRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PdfGeneratorPort pdfGeneratorPort;

    public ProposalPdfService(
            ProposalRepositoryPort proposalRepositoryPort,
            ClientRepositoryPort clientRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            PdfGeneratorPort pdfGeneratorPort) {
        this.proposalRepositoryPort = proposalRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.pdfGeneratorPort = pdfGeneratorPort;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadPdf(Query query) {
        Proposal proposal = proposalRepositoryPort.findByIdAndUserId(query.proposalId(), query.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
        Client client = clientRepositoryPort.findByIdAndUserId(proposal.getClientId(), query.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        User user = loadUser(query.userId());
        return pdfGeneratorPort.generateProposalPdf(proposal, client, user);
    }

    private User loadUser(UUID userId) {
        return userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
