package com.invoiceflow.infrastructure.web;

import com.invoiceflow.domain.model.DiscountType;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceItem;
import com.invoiceflow.domain.model.InvoiceStatus;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final EditInvoiceUseCase editInvoiceUseCase;
    private final DeleteInvoiceUseCase deleteInvoiceUseCase;
    private final DuplicateInvoiceUseCase duplicateInvoiceUseCase;
    private final MarkInvoicePaidUseCase markInvoicePaidUseCase;
    private final CancelInvoiceUseCase cancelInvoiceUseCase;
    private final SearchInvoicesUseCase searchInvoicesUseCase;
    private final GetInvoiceUseCase getInvoiceUseCase;
    private final PreviewInvoiceUseCase previewInvoiceUseCase;
    private final DownloadInvoicePdfUseCase downloadInvoicePdfUseCase;
    private final SendInvoiceUseCase sendInvoiceUseCase;
    private final ExportInvoicesCsvUseCase exportInvoicesCsvUseCase;

    public InvoiceController(
            CreateInvoiceUseCase createInvoiceUseCase,
            EditInvoiceUseCase editInvoiceUseCase,
            DeleteInvoiceUseCase deleteInvoiceUseCase,
            DuplicateInvoiceUseCase duplicateInvoiceUseCase,
            MarkInvoicePaidUseCase markInvoicePaidUseCase,
            CancelInvoiceUseCase cancelInvoiceUseCase,
            SearchInvoicesUseCase searchInvoicesUseCase,
            GetInvoiceUseCase getInvoiceUseCase,
            PreviewInvoiceUseCase previewInvoiceUseCase,
            DownloadInvoicePdfUseCase downloadInvoicePdfUseCase,
            SendInvoiceUseCase sendInvoiceUseCase,
            ExportInvoicesCsvUseCase exportInvoicesCsvUseCase) {
        this.createInvoiceUseCase = createInvoiceUseCase;
        this.editInvoiceUseCase = editInvoiceUseCase;
        this.deleteInvoiceUseCase = deleteInvoiceUseCase;
        this.duplicateInvoiceUseCase = duplicateInvoiceUseCase;
        this.markInvoicePaidUseCase = markInvoicePaidUseCase;
        this.cancelInvoiceUseCase = cancelInvoiceUseCase;
        this.searchInvoicesUseCase = searchInvoicesUseCase;
        this.getInvoiceUseCase = getInvoiceUseCase;
        this.previewInvoiceUseCase = previewInvoiceUseCase;
        this.downloadInvoicePdfUseCase = downloadInvoicePdfUseCase;
        this.sendInvoiceUseCase = sendInvoiceUseCase;
        this.exportInvoicesCsvUseCase = exportInvoicesCsvUseCase;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponseDto> createInvoice(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody InvoiceRequestDto request) {

        List<CreateInvoiceUseCase.ItemCommand> items = request.items().stream()
                .map(item -> new CreateInvoiceUseCase.ItemCommand(
                        item.description(),
                        item.quantity(),
                        item.unitPrice(),
                        item.position()
                ))
                .collect(Collectors.toList());

        Invoice invoice = createInvoiceUseCase.createInvoice(new CreateInvoiceUseCase.Command(
                user,
                user.getId(),
                request.clientId(),
                request.issueDate(),
                request.dueDate(),
                request.currency(),
                items,
                request.discountType(),
                request.discountValue(),
                request.taxRate(),
                request.notes(),
                request.paymentTerms()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(InvoiceResponseDto.fromDomain(invoice));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponseDto> editInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody InvoiceRequestDto request) {

        List<CreateInvoiceUseCase.ItemCommand> items = request.items().stream()
                .map(item -> new CreateInvoiceUseCase.ItemCommand(
                        item.description(),
                        item.quantity(),
                        item.unitPrice(),
                        item.position()
                ))
                .collect(Collectors.toList());

        Invoice invoice = editInvoiceUseCase.editInvoice(new EditInvoiceUseCase.Command(
                id,
                user.getId(),
                request.clientId(),
                request.issueDate(),
                request.dueDate(),
                request.currency(),
                items,
                request.discountType(),
                request.discountValue(),
                request.taxRate(),
                request.notes(),
                request.paymentTerms()
        ));

        return ResponseEntity.ok(InvoiceResponseDto.fromDomain(invoice));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        deleteInvoiceUseCase.deleteInvoice(new DeleteInvoiceUseCase.Command(id, user.getId()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<InvoiceResponseDto> duplicateInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        Invoice invoice = duplicateInvoiceUseCase.duplicateInvoice(new DuplicateInvoiceUseCase.Command(id, user.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(InvoiceResponseDto.fromDomain(invoice));
    }

    @PostMapping("/{id}/mark-paid")
    public ResponseEntity<InvoiceResponseDto> markPaid(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        Invoice invoice = markInvoicePaidUseCase.markPaid(new MarkInvoicePaidUseCase.Command(id, user.getId()));
        return ResponseEntity.ok(InvoiceResponseDto.fromDomain(invoice));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<InvoiceResponseDto> sendInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        Invoice invoice = sendInvoiceUseCase.sendInvoice(new SendInvoiceUseCase.Command(id, user.getId()));
        return ResponseEntity.ok(InvoiceResponseDto.fromDomain(invoice));
    }

    @GetMapping(value = "/{id}/preview", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> previewInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        String html = previewInvoiceUseCase.previewInvoice(new PreviewInvoiceUseCase.Query(id, user.getId()));
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    @GetMapping(value = "/{id}/pdf", produces = "application/pdf")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {
        byte[] pdf = downloadInvoicePdfUseCase.downloadPdf(new DownloadInvoicePdfUseCase.Query(id, user.getId()));
        Invoice invoice = getInvoiceUseCase.getInvoice(new GetInvoiceUseCase.Query(id, user.getId()));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + invoice.getInvoiceNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<InvoiceResponseDto> cancelInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        Invoice invoice = cancelInvoiceUseCase.cancelInvoice(new CancelInvoiceUseCase.Command(id, user.getId()));
        return ResponseEntity.ok(InvoiceResponseDto.fromDomain(invoice));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponseDto> getInvoice(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        Invoice invoice = getInvoiceUseCase.getInvoice(new GetInvoiceUseCase.Query(id, user.getId()));
        return ResponseEntity.ok(InvoiceResponseDto.fromDomain(invoice));
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceResponseDto>> getInvoices(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "status", required = false) InvoiceStatus status,
            @RequestParam(value = "clientId", required = false) UUID clientId,
            @RequestParam(value = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Invoice> invoices = searchInvoicesUseCase.searchInvoices(new SearchInvoicesUseCase.Query(
                user.getId(),
                status,
                clientId,
                startDate,
                endDate,
                search,
                pageable
        ));

        return ResponseEntity.ok(invoices.map(InvoiceResponseDto::fromDomain));
    }

    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportCsv(@AuthenticationPrincipal User user) {
        byte[] csv = exportInvoicesCsvUseCase.exportCsv(
                new ExportInvoicesCsvUseCase.Query(user.getId()));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"invoices.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    // Invoice DTOs
    public record InvoiceRequestDto(
            @NotNull(message = "Client ID is required")
            UUID clientId,

            @NotNull(message = "Issue date is required")
            LocalDate issueDate,

            @NotNull(message = "Due date is required")
            LocalDate dueDate,

            String currency,

            @NotNull(message = "Invoice items list cannot be null")
            List<InvoiceItemRequestDto> items,

            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal taxRate,
            String notes,
            String paymentTerms
    ) {}

    public record InvoiceItemRequestDto(
            @NotBlank(message = "Item description is required")
            String description,

            @NotNull(message = "Quantity is required")
            @Min(value = 0, message = "Quantity cannot be negative")
            BigDecimal quantity,

            @NotNull(message = "Unit price is required")
            @Min(value = 0, message = "Unit price cannot be negative")
            BigDecimal unitPrice,

            int position
    ) {}

    public record InvoiceResponseDto(
            UUID id,
            UUID userId,
            UUID clientId,
            String invoiceNumber,
            String status,
            LocalDate issueDate,
            LocalDate dueDate,
            String currency,
            BigDecimal subtotal,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal taxRate,
            BigDecimal total,
            String notes,
            String paymentTerms,
            String chapaPaymentLink,
            LocalDateTime paidAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            int version,
            List<InvoiceItemResponseDto> items
    ) {
        public static InvoiceResponseDto fromDomain(Invoice invoice) {
            List<InvoiceItemResponseDto> itemDtos = invoice.getItems().stream()
                    .map(InvoiceItemResponseDto::fromDomain)
                    .collect(Collectors.toList());
 
            return new InvoiceResponseDto(
                    invoice.getId(),
                    invoice.getUserId(),
                    invoice.getClientId(),
                    invoice.getInvoiceNumber(),
                    invoice.getStatus().name(),
                    invoice.getIssueDate(),
                    invoice.getDueDate(),
                    invoice.getCurrency(),
                    invoice.getSubtotal(),
                    invoice.getDiscountType(),
                    invoice.getDiscountValue(),
                    invoice.getTaxRate(),
                    invoice.getTotal(),
                    invoice.getNotes(),
                    invoice.getPaymentTerms(),
                    invoice.getChapaPaymentLink(),
                    invoice.getPaidAt(),
                    invoice.getCreatedAt(),
                    invoice.getUpdatedAt(),
                    invoice.getVersion(),
                    itemDtos
            );
        }
    }

    public record InvoiceItemResponseDto(
            UUID id,
            UUID invoiceId,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal total,
            int position
    ) {
        public static InvoiceItemResponseDto fromDomain(InvoiceItem item) {
            return new InvoiceItemResponseDto(
                    item.getId(),
                    item.getInvoiceId(),
                    item.getDescription(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getTotal(),
                    item.getPosition()
            );
        }
    }
}
