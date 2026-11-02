package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.BusinessRuleViolationException;
import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceStatus;
import com.invoiceflow.domain.model.Plan;
import com.invoiceflow.domain.port.in.*;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class ClientService implements
        AddClientUseCase,
        EditClientUseCase,
        DeleteClientUseCase,
        SearchClientsUseCase,
        ViewClientHistoryUseCase {

    private final ClientRepositoryPort clientRepositoryPort;
    private final InvoiceRepositoryPort invoiceRepositoryPort;

    public ClientService(ClientRepositoryPort clientRepositoryPort, InvoiceRepositoryPort invoiceRepositoryPort) {
        this.clientRepositoryPort = clientRepositoryPort;
        this.invoiceRepositoryPort = invoiceRepositoryPort;
    }

    @Override
    @Transactional
    public Client addClient(AddClientUseCase.Command command) {
        if (command.user().getPlan() == Plan.FREE) {
            long count = clientRepositoryPort.countByUserId(command.userId());
            if (count >= 3) {
                throw new BusinessRuleViolationException(
                        "Free plan allows max 3 clients. Upgrade to STARTER for unlimited.");
            }
        }

        if (clientRepositoryPort.existsByUserIdAndEmail(command.userId(), command.email())) {
            throw new BusinessRuleViolationException("Client with this email already registered");
        }
        Client client = Client.create(
                command.userId(),
                command.name(),
                command.email(),
                command.company(),
                command.address(),
                command.currency()
        );
        return clientRepositoryPort.save(client);
    }

    @Override
    @Transactional
    public Client editClient(EditClientUseCase.Command command) {
        Client client = clientRepositoryPort.findByIdAndUserId(command.clientId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (clientRepositoryPort.existsByUserIdAndEmailAndIdNot(command.userId(), command.email(), command.clientId())) {
            throw new BusinessRuleViolationException("Another client with this email already exists");
        }

        client.update(
                command.name(),
                command.email(),
                command.company(),
                command.address(),
                command.currency()
        );

        return clientRepositoryPort.save(client);
    }

    @Override
    @Transactional
    public void deleteClient(DeleteClientUseCase.Command command) {
        Client client = clientRepositoryPort.findByIdAndUserId(command.clientId(), command.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (invoiceRepositoryPort.existsActiveInvoicesForClient(command.clientId())) {
            throw new BusinessRuleViolationException("Cannot delete client: active invoices exist");
        }

        clientRepositoryPort.delete(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Client> searchClients(SearchClientsUseCase.Query query) {
        return clientRepositoryPort.findByUserIdAndSearch(query.userId(), query.searchQuery(), query.pageable());
    }

    @Override
    @Transactional(readOnly = true)
    public ViewClientHistoryUseCase.ClientHistory viewHistory(ViewClientHistoryUseCase.Query query) {
        Client client = clientRepositoryPort.findByIdAndUserId(query.clientId(), query.userId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Fetch all invoices for this client (using a large page size for history)
        Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Invoice> invoicePage = invoiceRepositoryPort.findByUserIdAndFilters(
                query.userId(),
                null,
                query.clientId(),
                null,
                null,
                null,
                pageable
        );
        List<Invoice> invoices = invoicePage.getContent();

        // Calculate total billed (sum totals of SENT, PAID, OVERDUE invoices)
        BigDecimal totalBilled = invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.SENT ||
                               inv.getStatus() == InvoiceStatus.PAID ||
                               inv.getStatus() == InvoiceStatus.OVERDUE)
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ViewClientHistoryUseCase.ClientHistory(client, invoices, totalBilled);
    }
}
