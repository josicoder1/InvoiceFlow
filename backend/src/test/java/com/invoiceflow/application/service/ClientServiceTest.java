package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.BusinessRuleViolationException;
import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Plan;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.AddClientUseCase;
import com.invoiceflow.domain.port.in.DeleteClientUseCase;
import com.invoiceflow.domain.port.in.EditClientUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClientServiceTest {

    private ClientRepositoryPort clientRepositoryPort;
    private InvoiceRepositoryPort invoiceRepositoryPort;
    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientRepositoryPort = mock(ClientRepositoryPort.class);
        invoiceRepositoryPort = mock(InvoiceRepositoryPort.class);
        clientService = new ClientService(clientRepositoryPort, invoiceRepositoryPort);
    }

    private User freeUser(UUID userId) {
        return User.reconstitute(userId, "user@test.com", "hash", null, "Test User",
                null, null, null, null, "USD", null, null, Plan.FREE,
                true, 3, 7, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void testAddClientSuccess() {
        UUID userId = UUID.randomUUID();
        AddClientUseCase.Command command = new AddClientUseCase.Command(
                freeUser(userId), userId, "Test Client", "test@client.com", "Client Co", "Client Address", "USD"
        );

        when(clientRepositoryPort.countByUserId(userId)).thenReturn(0L);
        when(clientRepositoryPort.existsByUserIdAndEmail(userId, "test@client.com")).thenReturn(false);
        when(clientRepositoryPort.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Client result = clientService.addClient(command);

        assertNotNull(result);
        assertEquals("Test Client", result.getName());
        verify(clientRepositoryPort, times(1)).save(any(Client.class));
    }

    @Test
    void testAddClientThrowsIfEmailRegistered() {
        UUID userId = UUID.randomUUID();
        AddClientUseCase.Command command = new AddClientUseCase.Command(
                freeUser(userId), userId, "Test Client", "test@client.com", "Client Co", "Client Address", "USD"
        );

        when(clientRepositoryPort.countByUserId(userId)).thenReturn(0L);
        when(clientRepositoryPort.existsByUserIdAndEmail(userId, "test@client.com")).thenReturn(true);

        assertThrows(BusinessRuleViolationException.class, () -> clientService.addClient(command));
        verify(clientRepositoryPort, never()).save(any(Client.class));
    }

    @Test
    void testDeleteClientSuccess() {
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        DeleteClientUseCase.Command command = new DeleteClientUseCase.Command(clientId, userId);
        Client client = Client.create(userId, "Test Client", "test@client.com", null, null, null);

        when(clientRepositoryPort.findByIdAndUserId(clientId, userId)).thenReturn(Optional.of(client));
        when(invoiceRepositoryPort.existsActiveInvoicesForClient(clientId)).thenReturn(false);

        clientService.deleteClient(command);

        verify(clientRepositoryPort, times(1)).delete(client);
    }

    @Test
    void testDeleteClientThrowsIfActiveInvoicesExist() {
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        DeleteClientUseCase.Command command = new DeleteClientUseCase.Command(clientId, userId);
        Client client = Client.create(userId, "Test Client", "test@client.com", null, null, null);

        when(clientRepositoryPort.findByIdAndUserId(clientId, userId)).thenReturn(Optional.of(client));
        when(invoiceRepositoryPort.existsActiveInvoicesForClient(clientId)).thenReturn(true);

        assertThrows(BusinessRuleViolationException.class, () -> clientService.deleteClient(command));
        verify(clientRepositoryPort, never()).delete(any(Client.class));
    }
}
