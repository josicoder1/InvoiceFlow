package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.BusinessRuleViolationException;
import com.invoiceflow.domain.exception.ConflictException;
import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.*;
import com.invoiceflow.domain.port.in.*;
import com.invoiceflow.domain.port.out.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProposalService implements
        CreateProposalUseCase,
        EditProposalUseCase,
        DeleteProposalUseCase,
        SendProposalUseCase,
        GetProposalUseCase,
        SearchProposalsUseCase,
        ViewProposalByTokenUseCase,
        AcceptProposalUseCase,
        DeclineProposalUseCase,
        ConvertProposalToInvoiceUseCase,
        GetProposalTemplatesUseCase {

    private final ProposalRepositoryPort proposalRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final ProposalTemplateRepositoryPort proposalTemplateRepositoryPort;
    private final OutboxEventPort outboxEventPort;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public ProposalService(
            ProposalRepositoryPort proposalRepositoryPort,
            ClientRepositoryPort clientRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            InvoiceRepositoryPort invoiceRepositoryPort,
            ProposalTemplateRepositoryPort proposalTemplateRepositoryPort,
            OutboxEventPort outboxEventPort) {
        this.proposalRepositoryPort = proposalRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.invoiceRepositoryPort = invoiceRepositoryPort;
        this.proposalTemplateRepositoryPort = proposalTemplateRepositoryPort;
        this.outboxEventPort = outboxEventPort;
    }

    @Override
    @Transactional
    public Proposal createProposal(CreateProposalUseCase.Command command) {
        clientRepositoryPort.findByIdAndUserId(command.clientId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        List<ProposalItem> items = toItems(command.items());
        String proposalNumber = generateNextProposalNumber(command.userId());

        Proposal proposal = Proposal.create(
                command.userId(), command.clientId(), proposalNumber, command.title(),
                command.overview(), command.scope(), command.timeline(),
                command.pricingNotes(), command.validUntil(), items,
                command.discountType(), command.discountValue(), command.taxRate());

        return proposalRepositoryPort.save(proposal);
    }

    @Override
    @Transactional
    public Proposal editProposal(EditProposalUseCase.Command command) {
        Proposal proposal = proposalRepositoryPort.findByIdAndUserId(command.proposalId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        if (proposal.getVersion() != command.version()) {
            throw new ConflictException("Proposal was modified by another request. Please refresh and try again.");
        }

        clientRepositoryPort.findByIdAndUserId(command.clientId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        List<ProposalItem> items = toItems(command.items());

        proposal.edit(command.clientId(), command.title(), command.overview(),
                command.scope(), command.timeline(), command.pricingNotes(),
                command.validUntil(), items, command.discountType(),
                command.discountValue(), command.taxRate());

        return proposalRepositoryPort.save(proposal);
    }

    @Override
    @Transactional
    public void deleteProposal(DeleteProposalUseCase.Command command) {
        Proposal proposal = proposalRepositoryPort.findByIdAndUserId(command.proposalId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        if (proposal.getStatus() == ProposalStatus.ACCEPTED) {
            throw new BusinessRuleViolationException("Cannot delete an ACCEPTED proposal");
        }
        if (proposal.getStatus() != ProposalStatus.DRAFT && proposal.getStatus() != ProposalStatus.DECLINED) {
            throw new BusinessRuleViolationException("Can only delete DRAFT or DECLINED proposals");
        }

        proposalRepositoryPort.delete(proposal);
    }

    @Override
    @Transactional
    public Proposal sendProposal(SendProposalUseCase.Command command) {
        Proposal proposal = proposalRepositoryPort.findByIdAndUserId(command.proposalId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        Client client = clientRepositoryPort.findByIdAndUserId(proposal.getClientId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        User user = userRepositoryPort.findById(command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        proposal.send();
        Proposal saved = proposalRepositoryPort.save(proposal);

        String publicLink = frontendUrl + "/p/" + saved.getPublicToken();
        String businessName = businessName(user);

        outboxEventPort.publish("Proposal", saved.getId(), "PROPOSAL_SENT", Map.of(
                "toEmail", client.getEmail(),
                "clientName", client.getName(),
                "proposalTitle", saved.getTitle(),
                "businessName", businessName,
                "publicLink", publicLink));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Proposal getProposal(GetProposalUseCase.Query query) {
        return proposalRepositoryPort.findByIdAndUserId(query.proposalId(), query.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Proposal> searchProposals(SearchProposalsUseCase.Query query) {
        return proposalRepositoryPort.findByUserIdAndFilters(
                query.userId(), query.status(), query.clientId(), query.searchQuery(), query.pageable());
    }

    @Override
    @Transactional
    public Proposal viewByToken(String publicToken) {
        Proposal proposal = proposalRepositoryPort.findByPublicToken(publicToken)
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        boolean firstView = proposal.recordViewed();
        Proposal saved = proposalRepositoryPort.save(proposal);

        if (firstView) {
            Client client = clientRepositoryPort.findById(proposal.getClientId())
                    .orElse(null);
            User owner = userRepositoryPort.findById(proposal.getUserId()).orElse(null);
            if (client != null && owner != null) {
                outboxEventPort.publish("Proposal", proposal.getId(), "PROPOSAL_VIEWED", Map.of(
                        "ownerEmail", owner.getEmail(),
                        "clientName", client.getName(),
                        "clientCompany", client.getCompany() != null ? client.getCompany() : "",
                        "proposalTitle", proposal.getTitle()
                ));
            }
        }

        return saved;
    }

    @Override
    @Transactional
    public Proposal acceptProposal(AcceptProposalUseCase.Command command) {
        Proposal proposal = proposalRepositoryPort.findByPublicToken(command.publicToken())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        proposal.accept(command.signatureData());
        Proposal saved = proposalRepositoryPort.save(proposal);

        Client client = clientRepositoryPort.findById(proposal.getClientId()).orElse(null);
        User owner = userRepositoryPort.findById(proposal.getUserId()).orElse(null);
        if (client != null && owner != null) {
            outboxEventPort.publish("Proposal", proposal.getId(), "PROPOSAL_ACCEPTED", Map.of(
                    "ownerEmail", owner.getEmail(),
                    "proposalTitle", proposal.getTitle(),
                    "proposalNumber", proposal.getProposalNumber(),
                    "clientName", client.getName(),
                    "signedAt", saved.getSignedAt().toString()
            ));
        }

        return saved;
    }

    @Override
    @Transactional
    public Proposal declineProposal(DeclineProposalUseCase.Command command) {
        Proposal proposal = proposalRepositoryPort.findByPublicToken(command.publicToken())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        proposal.decline();
        Proposal saved = proposalRepositoryPort.save(proposal);

        Client client = clientRepositoryPort.findById(proposal.getClientId()).orElse(null);
        User owner = userRepositoryPort.findById(proposal.getUserId()).orElse(null);
        if (client != null && owner != null) {
            outboxEventPort.publish("Proposal", proposal.getId(), "PROPOSAL_DECLINED", Map.of(
                    "ownerEmail", owner.getEmail(),
                    "proposalTitle", proposal.getTitle(),
                    "clientName", client.getName(),
                    "reason", command.reason() != null ? command.reason() : ""
            ));
        }

        return saved;
    }

    @Override
    @Transactional
    public Invoice convertToInvoice(ConvertProposalToInvoiceUseCase.Command command) {
        Proposal proposal = proposalRepositoryPort.findByIdAndUserId(command.proposalId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Proposal not found"));

        if (proposal.getConvertedToInvoiceId() != null) {
            throw new BusinessRuleViolationException("Proposal has already been converted to an invoice");
        }

        String invoiceNumber = generateNextInvoiceNumber(command.userId());

        List<InvoiceItem> items = proposal.getItems().stream()
                .map(pi -> InvoiceItem.create(
                        pi.getDescription(),
                        pi.getQuantity() != null ? pi.getQuantity() : java.math.BigDecimal.ONE,
                        pi.getUnitPrice() != null ? pi.getUnitPrice() : java.math.BigDecimal.ZERO,
                        pi.getPosition()))
                .collect(Collectors.toList());

        Invoice invoice = Invoice.create(
                command.userId(),
                proposal.getClientId(),
                invoiceNumber,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                "USD",
                items,
                proposal.getDiscountType(),
                proposal.getDiscountValue(),
                proposal.getTaxRate(),
                proposal.getPricingNotes(),
                null);

        Invoice savedInvoice = invoiceRepositoryPort.save(invoice);

        proposal.markConverted(savedInvoice.getId());
        proposalRepositoryPort.save(proposal);

        return savedInvoice;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetProposalTemplatesUseCase.ProposalTemplate> getTemplates() {
        return proposalTemplateRepositoryPort.findAllTemplates().stream()
                .map(t -> new GetProposalTemplatesUseCase.ProposalTemplate(
                        t.id(), t.name(), t.overview(),
                        t.scope(), t.timeline(), t.pricingNotes()))
                .collect(Collectors.toList());
    }

    private List<ProposalItem> toItems(List<CreateProposalUseCase.ItemCommand> items) {
        if (items == null) return new ArrayList<>();
        return items.stream()
                .map(i -> ProposalItem.create(i.description(), i.quantity(), i.unitPrice(), i.position()))
                .collect(Collectors.toList());
    }

    private String businessName(User user) {
        return (user.getBusinessName() != null && !user.getBusinessName().isBlank())
                ? user.getBusinessName() : user.getFullName();
    }

    private String generateNextProposalNumber(UUID userId) {
        Optional<String> maxOpt = proposalRepositoryPort.findMaxProposalNumberByUserId(userId);
        if (maxOpt.isEmpty() || maxOpt.get() == null) return "PROP-0001";
        String max = maxOpt.get();
        if (max.matches("^PROP-\\d+$")) {
            try {
                int num = Integer.parseInt(max.substring(5));
                return String.format("PROP-%04d", num + 1);
            } catch (NumberFormatException e) {
                return "PROP-0001";
            }
        }
        return "PROP-0001";
    }

    private String generateNextInvoiceNumber(UUID userId) {
        Optional<String> maxOpt = invoiceRepositoryPort.findMaxInvoiceNumberByUserId(userId);
        if (maxOpt.isEmpty()) return "INV-0001";
        String max = maxOpt.get();
        if (max.matches("^INV-\\d+$")) {
            try {
                int num = Integer.parseInt(max.substring(4));
                return String.format("INV-%04d", num + 1);
            } catch (NumberFormatException e) {
                return "INV-0001";
            }
        }
        return "INV-0001";
    }
}
