package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.BusinessRuleViolationException;
import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceItem;
import com.invoiceflow.domain.model.InvoiceStatus;
import com.invoiceflow.domain.model.Plan;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.CreateInvoiceUseCase;
import com.invoiceflow.domain.port.in.DeleteInvoiceUseCase;
import com.invoiceflow.domain.port.in.DuplicateInvoiceUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InvoiceServiceTest {

    private InvoiceRepositoryPort invoiceRepositoryPort;
    private ClientRepositoryPort clientRepositoryPort;
    private InvoiceService invoiceService;

    @BeforeEach
    void setUp() {
        invoiceRepositoryPort = mock(InvoiceRepositoryPort.class);
        clientRepositoryPort = mock(ClientRepositoryPort.class);
        invoiceService = new InvoiceService(invoiceRepositoryPort, clientRepositoryPort);
    }

    private User freeUser(UUID userId) {
        return User.reconstitute(userId, "user@test.com", "hash", null, "Test User",
                null, null, null, null, "USD", null, null, Plan.FREE,
                true, 3, 7, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void testCreateInvoiceAutoNumberingInitial() {
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Client client = Client.create(userId, "Client", "client@mail.com", null, null, null);

        CreateInvoiceUseCase.Command command = new CreateInvoiceUseCase.Command(
                freeUser(userId), userId, clientId, LocalDate.now(), LocalDate.now().plusDays(30), "USD",
                Collections.singletonList(new CreateInvoiceUseCase.ItemCommand("Description", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, null, null, null, null
        );

        when(invoiceRepositoryPort.countByUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(0L);
        when(clientRepositoryPort.findByIdAndUserId(clientId, userId)).thenReturn(Optional.of(client));
        when(invoiceRepositoryPort.findMaxInvoiceNumberByUserId(userId)).thenReturn(Optional.empty());
        when(invoiceRepositoryPort.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice result = invoiceService.createInvoice(command);

        assertNotNull(result);
        assertEquals("INV-0001", result.getInvoiceNumber());
        verify(invoiceRepositoryPort, times(1)).save(any(Invoice.class));
    }

    @Test
    void testCreateInvoiceAutoNumberingIncrements() {
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        Client client = Client.create(userId, "Client", "client@mail.com", null, null, null);

        CreateInvoiceUseCase.Command command = new CreateInvoiceUseCase.Command(
                freeUser(userId), userId, clientId, LocalDate.now(), LocalDate.now().plusDays(30), "USD",
                Collections.singletonList(new CreateInvoiceUseCase.ItemCommand("Description", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, null, null, null, null
        );

        when(invoiceRepositoryPort.countByUserIdAndCreatedAtBetween(any(), any(), any())).thenReturn(0L);
        when(clientRepositoryPort.findByIdAndUserId(clientId, userId)).thenReturn(Optional.of(client));
        when(invoiceRepositoryPort.findMaxInvoiceNumberByUserId(userId)).thenReturn(Optional.of("INV-0042"));
        when(invoiceRepositoryPort.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice result = invoiceService.createInvoice(command);

        assertNotNull(result);
        assertEquals("INV-0043", result.getInvoiceNumber());
    }

    @Test
    void testDeleteInvoiceThrowsIfNotDraftOrCancelled() {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.reconstitute(
                invoiceId, userId, UUID.randomUUID(), "INV-0001", null, InvoiceStatus.SENT,
                LocalDate.now(), LocalDate.now().plusDays(30), "USD", BigDecimal.TEN,
                null, null, null, BigDecimal.TEN, null, null, null, null, null, null, null,
                null, null, 0, null
        );

        when(invoiceRepositoryPort.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));

        assertThrows(BusinessRuleViolationException.class, () ->
                invoiceService.deleteInvoice(new DeleteInvoiceUseCase.Command(invoiceId, userId))
        );
        verify(invoiceRepositoryPort, never()).delete(any(Invoice.class));
    }

    @Test
    void testDuplicateInvoiceSuccess() {
        UUID userId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();
        Invoice invoice = Invoice.create(
                userId, UUID.randomUUID(), "INV-0001", LocalDate.now(), LocalDate.now().plusDays(30), "USD",
                Collections.singletonList(InvoiceItem.create("item", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, null, null, null, null
        );

        when(invoiceRepositoryPort.findByIdAndUserId(invoiceId, userId)).thenReturn(Optional.of(invoice));
        when(invoiceRepositoryPort.findMaxInvoiceNumberByUserId(userId)).thenReturn(Optional.of("INV-0042"));
        when(invoiceRepositoryPort.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Invoice duplicated = invoiceService.duplicateInvoice(new DuplicateInvoiceUseCase.Command(invoiceId, userId));

        assertNotNull(duplicated);
        assertEquals("INV-0043", duplicated.getInvoiceNumber());
        assertEquals(InvoiceStatus.DRAFT, duplicated.getStatus());
    }
}
