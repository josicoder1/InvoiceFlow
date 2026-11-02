package com.invoiceflow.infrastructure.web;

import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final AddClientUseCase addClientUseCase;
    private final EditClientUseCase editClientUseCase;
    private final DeleteClientUseCase deleteClientUseCase;
    private final SearchClientsUseCase searchClientsUseCase;
    private final ViewClientHistoryUseCase viewClientHistoryUseCase;

    public ClientController(
            AddClientUseCase addClientUseCase,
            EditClientUseCase editClientUseCase,
            DeleteClientUseCase deleteClientUseCase,
            SearchClientsUseCase searchClientsUseCase,
            ViewClientHistoryUseCase viewClientHistoryUseCase) {
        this.addClientUseCase = addClientUseCase;
        this.editClientUseCase = editClientUseCase;
        this.deleteClientUseCase = deleteClientUseCase;
        this.searchClientsUseCase = searchClientsUseCase;
        this.viewClientHistoryUseCase = viewClientHistoryUseCase;
    }

    @PostMapping
    public ResponseEntity<ClientResponseDto> addClient(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ClientRequestDto request) {

        Client client = addClientUseCase.addClient(new AddClientUseCase.Command(
                user,
                user.getId(),
                request.name(),
                request.email(),
                request.company(),
                request.address(),
                request.currency()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(ClientResponseDto.fromDomain(client));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponseDto> editClient(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ClientRequestDto request) {

        Client client = editClientUseCase.editClient(new EditClientUseCase.Command(
                id,
                user.getId(),
                request.name(),
                request.email(),
                request.company(),
                request.address(),
                request.currency()
        ));

        return ResponseEntity.ok(ClientResponseDto.fromDomain(client));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        deleteClientUseCase.deleteClient(new DeleteClientUseCase.Command(id, user.getId()));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<ClientResponseDto>> getClients(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Client> clients = searchClientsUseCase.searchClients(new SearchClientsUseCase.Query(
                user.getId(),
                search,
                pageable
        ));

        return ResponseEntity.ok(clients.map(ClientResponseDto::fromDomain));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponseDto> getClient(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        // Use history endpoint command to locate the client under user auth scope
        ViewClientHistoryUseCase.ClientHistory history = viewClientHistoryUseCase.viewHistory(
                new ViewClientHistoryUseCase.Query(id, user.getId())
        );

        return ResponseEntity.ok(ClientResponseDto.fromDomain(history.client()));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ClientHistoryResponseDto> getClientHistory(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user) {

        ViewClientHistoryUseCase.ClientHistory history = viewClientHistoryUseCase.viewHistory(
                new ViewClientHistoryUseCase.Query(id, user.getId())
        );

        return ResponseEntity.ok(ClientHistoryResponseDto.fromDomain(history));
    }

    // Client DTOs
    public record ClientRequestDto(
            @NotBlank(message = "Client name is required")
            String name,

            @NotBlank(message = "Client email is required")
            @Email(message = "Invalid email format")
            String email,

            String company,
            String address,
            String currency
    ) {}

    public record ClientResponseDto(
            UUID id,
            UUID userId,
            String name,
            String email,
            String company,
            String address,
            String currency,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static ClientResponseDto fromDomain(Client client) {
            return new ClientResponseDto(
                    client.getId(),
                    client.getUserId(),
                    client.getName(),
                    client.getEmail(),
                    client.getCompany(),
                    client.getAddress(),
                    client.getCurrency(),
                    client.getCreatedAt(),
                    client.getUpdatedAt()
            );
        }
    }

    public record ClientHistoryResponseDto(
            ClientResponseDto client,
            List<InvoiceController.InvoiceResponseDto> invoices,
            BigDecimal totalBilled
    ) {
        public static ClientHistoryResponseDto fromDomain(ViewClientHistoryUseCase.ClientHistory history) {
            List<InvoiceController.InvoiceResponseDto> invoiceDtos = history.invoices().stream()
                    .map(InvoiceController.InvoiceResponseDto::fromDomain)
                    .collect(Collectors.toList());

            return new ClientHistoryResponseDto(
                    ClientResponseDto.fromDomain(history.client()),
                    invoiceDtos,
                    history.totalBilled()
            );
        }
    }
}
