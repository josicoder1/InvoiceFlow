package com.invoiceflow.infrastructure.web;

import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceItem;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.PayInvoiceByTokenUseCase;
import com.invoiceflow.domain.port.in.ViewInvoiceByTokenUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public/invoices")
public class PublicInvoiceController {

    private final ViewInvoiceByTokenUseCase viewInvoiceByTokenUseCase;
    private final PayInvoiceByTokenUseCase payInvoiceByTokenUseCase;
    private final ClientRepositoryPort clientRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    public PublicInvoiceController(
            ViewInvoiceByTokenUseCase viewInvoiceByTokenUseCase,
            PayInvoiceByTokenUseCase payInvoiceByTokenUseCase,
            ClientRepositoryPort clientRepositoryPort,
            UserRepositoryPort userRepositoryPort) {
        this.viewInvoiceByTokenUseCase = viewInvoiceByTokenUseCase;
        this.payInvoiceByTokenUseCase = payInvoiceByTokenUseCase;
        this.clientRepositoryPort = clientRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    @GetMapping("/{token}")
    public ResponseEntity<PublicInvoiceResponseDto> viewInvoice(@PathVariable String token) {
        Invoice invoice = viewInvoiceByTokenUseCase.viewByToken(token);
        return ResponseEntity.ok(toPublicDto(invoice));
    }

    @PostMapping("/{token}/pay")
    public ResponseEntity<PayResponseDto> payInvoice(@PathVariable String token) {
        PayInvoiceByTokenUseCase.PayResult result = payInvoiceByTokenUseCase.createCheckoutSession(token);
        return ResponseEntity.ok(new PayResponseDto(result.checkoutUrl(), result.sessionId()));
    }

    @PostMapping("/{token}/confirm-payment")
    public ResponseEntity<PublicInvoiceResponseDto> confirmPayment(
            @PathVariable String token,
            @RequestBody ConfirmPaymentRequest request) {
        payInvoiceByTokenUseCase.confirmPayment(token, request.txRef());
        Invoice invoice = viewInvoiceByTokenUseCase.viewByToken(token);
        return ResponseEntity.ok(toPublicDto(invoice));
    }

    private PublicInvoiceResponseDto toPublicDto(Invoice invoice) {
        Client client = clientRepositoryPort.findById(invoice.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        User owner = userRepositoryPort.findById(invoice.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<PublicInvoiceItemDto> items = invoice.getItems().stream()
                .map(i -> new PublicInvoiceItemDto(
                        i.getDescription(), i.getQuantity(), i.getUnitPrice(), i.getTotal(), i.getPosition()))
                .collect(Collectors.toList());

        return new PublicInvoiceResponseDto(
                invoice.getInvoiceNumber(),
                invoice.getStatus().name(),
                new PartyDto(
                        owner.getBusinessName() != null ? owner.getBusinessName() : owner.getFullName(),
                        owner.getEmail(),
                        owner.getBusinessAddress(),
                        owner.getBusinessLogoUrl()),
                new PartyDto(client.getName(), client.getEmail(), client.getAddress(), null),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getCurrency(),
                items,
                invoice.getSubtotal(),
                invoice.getDiscountType() != null ? invoice.getDiscountType().name() : null,
                invoice.getDiscountValue(),
                invoice.getTaxRate(),
                invoice.getTotal(),
                invoice.getNotes(),
                invoice.getPaymentTerms(),
                invoice.isPayable() && owner.isChapaConnected(),
                invoice.getPaidAt() != null
        );
    }

    public record PayResponseDto(String checkoutUrl, String sessionId) {}

    public record ConfirmPaymentRequest(String txRef) {}

    public record PartyDto(String name, String email, String address, String logoUrl) {}

    public record PublicInvoiceItemDto(
            String description, BigDecimal quantity, BigDecimal unitPrice,
            BigDecimal total, int position) {}

    public record PublicInvoiceResponseDto(
            String invoiceNumber,
            String status,
            PartyDto from,
            PartyDto to,
            LocalDate issueDate,
            LocalDate dueDate,
            String currency,
            List<PublicInvoiceItemDto> items,
            BigDecimal subtotal,
            String discountType,
            BigDecimal discountValue,
            BigDecimal taxRate,
            BigDecimal total,
            String notes,
            String paymentTerms,
            boolean canPay,
            boolean isPaid
    ) {}
}
