package com.invoiceflow.infrastructure.web;

import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Proposal;
import com.invoiceflow.domain.model.ProposalItem;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.AcceptProposalUseCase;
import com.invoiceflow.domain.port.in.DeclineProposalUseCase;
import com.invoiceflow.domain.port.in.ViewProposalByTokenUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/public/proposals")
public class PublicProposalController {

    private final ViewProposalByTokenUseCase viewProposalByTokenUseCase;
    private final AcceptProposalUseCase acceptProposalUseCase;
    private final DeclineProposalUseCase declineProposalUseCase;
    private final ClientRepositoryPort clientRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    public PublicProposalController(
            ViewProposalByTokenUseCase viewProposalByTokenUseCase,
            AcceptProposalUseCase acceptProposalUseCase,
            DeclineProposalUseCase declineProposalUseCase,
            ClientRepositoryPort clientRepositoryPort,
            UserRepositoryPort userRepositoryPort) {
        this.viewProposalByTokenUseCase = viewProposalByTokenUseCase;
        this.acceptProposalUseCase = acceptProposalUseCase;
        this.declineProposalUseCase = declineProposalUseCase;
        this.clientRepositoryPort = clientRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    @GetMapping("/{token}")
    public ResponseEntity<PublicProposalResponseDto> viewProposal(@PathVariable String token) {
        Proposal proposal = viewProposalByTokenUseCase.viewByToken(token);
        return ResponseEntity.ok(toPublicDto(proposal));
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<PublicProposalResponseDto> acceptProposal(
            @PathVariable String token,
            @Valid @RequestBody AcceptRequestDto request) {
        Proposal proposal = acceptProposalUseCase.acceptProposal(
                new AcceptProposalUseCase.Command(token, request.signatureData()));
        return ResponseEntity.ok(toPublicDto(proposal));
    }

    @PostMapping("/{token}/decline")
    public ResponseEntity<PublicProposalResponseDto> declineProposal(
            @PathVariable String token,
            @RequestBody(required = false) DeclineRequestDto request) {
        String reason = request != null ? request.reason() : null;
        Proposal proposal = declineProposalUseCase.declineProposal(
                new DeclineProposalUseCase.Command(token, reason));
        return ResponseEntity.ok(toPublicDto(proposal));
    }

    private PublicProposalResponseDto toPublicDto(Proposal proposal) {
        Client client = clientRepositoryPort.findById(proposal.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        User owner = userRepositoryPort.findById(proposal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<PublicProposalItemDto> items = proposal.getItems().stream()
                .map(i -> new PublicProposalItemDto(
                        i.getDescription(), i.getQuantity(), i.getUnitPrice(), i.getTotal(), i.getPosition()))
                .collect(Collectors.toList());

        return new PublicProposalResponseDto(
                proposal.getProposalNumber(),
                proposal.getTitle(),
                proposal.getStatus().name(),
                new PartyDto(
                        owner.getBusinessName() != null ? owner.getBusinessName() : owner.getFullName(),
                        owner.getEmail(),
                        owner.getBusinessAddress(),
                        owner.getBusinessLogoUrl()),
                new PartyDto(client.getName(), client.getEmail(), client.getAddress(), null),
                proposal.getOverview(),
                proposal.getScope(),
                proposal.getTimeline(),
                proposal.getPricingNotes(),
                items,
                proposal.getSubtotal(),
                proposal.getDiscountType() != null ? proposal.getDiscountType().name() : null,
                proposal.getDiscountValue(),
                proposal.getTaxRate(),
                proposal.getTotal(),
                proposal.getValidUntil(),
                proposal.isExpired(),
                proposal.isActionable(),
                proposal.getSignedAt() != null
        );
    }

    public record AcceptRequestDto(
            @NotBlank(message = "Signature data is required") String signatureData
    ) {}

    public record DeclineRequestDto(String reason) {}

    public record PartyDto(String name, String email, String address, String logoUrl) {}

    public record PublicProposalItemDto(
            String description, BigDecimal quantity, BigDecimal unitPrice,
            BigDecimal total, int position) {}

    public record PublicProposalResponseDto(
            String proposalNumber, String title, String status,
            PartyDto from, PartyDto to,
            String overview, String scope, String timeline, String pricingNotes,
            List<PublicProposalItemDto> items,
            BigDecimal subtotal, String discountType, BigDecimal discountValue,
            BigDecimal taxRate, BigDecimal total,
            LocalDate validUntil, boolean isExpired, boolean isActionable, boolean hasSigned
    ) {}
}
