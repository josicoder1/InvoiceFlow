package com.invoiceflow.infrastructure.web;

import com.invoiceflow.domain.model.*;
import com.invoiceflow.domain.port.in.*;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/proposals")
public class ProposalController {

    private final CreateProposalUseCase createProposalUseCase;
    private final EditProposalUseCase editProposalUseCase;
    private final DeleteProposalUseCase deleteProposalUseCase;
    private final SendProposalUseCase sendProposalUseCase;
    private final GetProposalUseCase getProposalUseCase;
    private final SearchProposalsUseCase searchProposalsUseCase;
    private final ConvertProposalToInvoiceUseCase convertProposalToInvoiceUseCase;
    private final GetProposalTemplatesUseCase getProposalTemplatesUseCase;
    private final DownloadProposalPdfUseCase downloadProposalPdfUseCase;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public ProposalController(
            CreateProposalUseCase createProposalUseCase,
            EditProposalUseCase editProposalUseCase,
            DeleteProposalUseCase deleteProposalUseCase,
            SendProposalUseCase sendProposalUseCase,
            GetProposalUseCase getProposalUseCase,
            SearchProposalsUseCase searchProposalsUseCase,
            ConvertProposalToInvoiceUseCase convertProposalToInvoiceUseCase,
            GetProposalTemplatesUseCase getProposalTemplatesUseCase,
            DownloadProposalPdfUseCase downloadProposalPdfUseCase) {
        this.createProposalUseCase = createProposalUseCase;
        this.editProposalUseCase = editProposalUseCase;
        this.deleteProposalUseCase = deleteProposalUseCase;
        this.sendProposalUseCase = sendProposalUseCase;
        this.getProposalUseCase = getProposalUseCase;
        this.searchProposalsUseCase = searchProposalsUseCase;
        this.convertProposalToInvoiceUseCase = convertProposalToInvoiceUseCase;
        this.getProposalTemplatesUseCase = getProposalTemplatesUseCase;
        this.downloadProposalPdfUseCase = downloadProposalPdfUseCase;
    }

    @PostMapping
    public ResponseEntity<ProposalResponseDto> createProposal(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProposalRequestDto request) {

        Proposal proposal = createProposalUseCase.createProposal(new CreateProposalUseCase.Command(
                user.getId(), request.clientId(), request.title(),
                request.overview(), request.scope(), request.timeline(),
                request.pricingNotes(), request.validUntil(), buildItemCommands(request.items()),
                request.discountType(), request.discountValue(), request.taxRate()));

        return ResponseEntity.status(HttpStatus.CREATED).body(ProposalResponseDto.fromDomain(proposal, frontendUrl));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProposalResponseDto> editProposal(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProposalRequestDto request) {

        Proposal proposal = editProposalUseCase.editProposal(new EditProposalUseCase.Command(
                id, user.getId(), request.version(), request.clientId(), request.title(),
                request.overview(), request.scope(), request.timeline(),
                request.pricingNotes(), request.validUntil(), buildItemCommands(request.items()),
                request.discountType(), request.discountValue(), request.taxRate()));

        return ResponseEntity.ok(ProposalResponseDto.fromDomain(proposal, frontendUrl));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProposal(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        deleteProposalUseCase.deleteProposal(new DeleteProposalUseCase.Command(id, user.getId()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<ProposalResponseDto> sendProposal(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        Proposal proposal = sendProposalUseCase.sendProposal(new SendProposalUseCase.Command(id, user.getId()));
        return ResponseEntity.ok(ProposalResponseDto.fromDomain(proposal, frontendUrl));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProposalResponseDto> getProposal(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        Proposal proposal = getProposalUseCase.getProposal(new GetProposalUseCase.Query(id, user.getId()));
        return ResponseEntity.ok(ProposalResponseDto.fromDomain(proposal, frontendUrl));
    }

    @GetMapping
    public ResponseEntity<Page<ProposalResponseDto>> getProposals(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) ProposalStatus status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Proposal> proposals = searchProposalsUseCase.searchProposals(
                new SearchProposalsUseCase.Query(user.getId(), status, clientId, search, pageable));
        return ResponseEntity.ok(proposals.map(p -> ProposalResponseDto.fromDomain(p, frontendUrl)));
    }

    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        byte[] pdf = downloadProposalPdfUseCase.downloadPdf(new DownloadProposalPdfUseCase.Query(id, user.getId()));
        Proposal proposal = getProposalUseCase.getProposal(new GetProposalUseCase.Query(id, user.getId()));
        String filename = (proposal.getProposalNumber() != null ? proposal.getProposalNumber() : proposal.getTitle())
                .replaceAll("[^a-zA-Z0-9-_]", "_") + ".pdf";
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/convert-to-invoice")
    public ResponseEntity<InvoiceController.InvoiceResponseDto> convertToInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        Invoice invoice = convertProposalToInvoiceUseCase.convertToInvoice(
                new ConvertProposalToInvoiceUseCase.Command(id, user.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(InvoiceController.InvoiceResponseDto.fromDomain(invoice));
    }

    @GetMapping("/templates")
    public ResponseEntity<List<GetProposalTemplatesUseCase.ProposalTemplate>> getTemplates() {
        return ResponseEntity.ok(getProposalTemplatesUseCase.getTemplates());
    }

    private List<CreateProposalUseCase.ItemCommand> buildItemCommands(List<ProposalItemRequestDto> items) {
        if (items == null) return List.of();
        return items.stream()
                .map(i -> new CreateProposalUseCase.ItemCommand(
                        i.description(), i.quantity(), i.unitPrice(), i.position()))
                .collect(Collectors.toList());
    }

    public record ProposalRequestDto(
            @NotNull(message = "Client ID is required") UUID clientId,
            @NotBlank(message = "Title is required") String title,
            String overview, String scope, String timeline,
            String pricingNotes, LocalDate validUntil,
            List<ProposalItemRequestDto> items,
            DiscountType discountType, BigDecimal discountValue, BigDecimal taxRate,
            int version
    ) {}

    public record ProposalItemRequestDto(
            @NotBlank(message = "Description is required") String description,
            BigDecimal quantity, BigDecimal unitPrice, int position
    ) {}

    public record ProposalResponseDto(
            UUID id, UUID userId, UUID clientId,
            String proposalNumber, String title, String publicToken, String publicLink, String status,
            String overview, String scope, String timeline,
            String pricingNotes, LocalDate validUntil,
            BigDecimal subtotal, DiscountType discountType, BigDecimal discountValue,
            BigDecimal taxRate, BigDecimal total,
            boolean hasSigned, LocalDateTime signedAt, LocalDateTime viewedAt,
            UUID convertedToInvoiceId,
            LocalDateTime createdAt, LocalDateTime updatedAt, int version,
            List<ProposalItemResponseDto> items
    ) {
        public static ProposalResponseDto fromDomain(Proposal p, String frontendUrl) {
            List<ProposalItemResponseDto> itemDtos = p.getItems().stream()
                    .map(ProposalItemResponseDto::fromDomain)
                    .collect(Collectors.toList());
            String publicLink = p.getPublicToken() != null ? frontendUrl + "/p/" + p.getPublicToken() : null;
            return new ProposalResponseDto(
                    p.getId(), p.getUserId(), p.getClientId(),
                    p.getProposalNumber(), p.getTitle(), p.getPublicToken(), publicLink, p.getStatus().name(),
                    p.getOverview(), p.getScope(), p.getTimeline(),
                    p.getPricingNotes(), p.getValidUntil(),
                    p.getSubtotal(), p.getDiscountType(), p.getDiscountValue(),
                    p.getTaxRate(), p.getTotal(),
                    p.getSignatureData() != null, p.getSignedAt(), p.getViewedAt(),
                    p.getConvertedToInvoiceId(),
                    p.getCreatedAt(), p.getUpdatedAt(), p.getVersion(), itemDtos);
        }
    }

    public record ProposalItemResponseDto(
            UUID id, UUID proposalId, String description,
            BigDecimal quantity, BigDecimal unitPrice, BigDecimal total, int position
    ) {
        public static ProposalItemResponseDto fromDomain(ProposalItem i) {
            return new ProposalItemResponseDto(
                    i.getId(), i.getProposalId(), i.getDescription(),
                    i.getQuantity(), i.getUnitPrice(), i.getTotal(), i.getPosition());
        }
    }
}
