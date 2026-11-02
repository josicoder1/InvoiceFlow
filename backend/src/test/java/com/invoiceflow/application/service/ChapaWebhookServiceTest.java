package com.invoiceflow.application.service;

import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceItem;
import com.invoiceflow.domain.model.InvoiceStatus;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import com.invoiceflow.domain.port.out.OutboxEventPort;
import com.invoiceflow.domain.port.out.PaymentPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChapaWebhookServiceTest {

    private PaymentPort paymentPort;
    private InvoiceRepositoryPort invoiceRepositoryPort;
    private ClientRepositoryPort clientRepositoryPort;
    private UserRepositoryPort userRepositoryPort;
    private OutboxEventPort outboxEventPort;
    private InvoicePaymentService invoicePaymentService;
    private ChapaWebhookService chapaWebhookService;

    @BeforeEach
    void setUp() {
        paymentPort = mock(PaymentPort.class);
        invoiceRepositoryPort = mock(InvoiceRepositoryPort.class);
        clientRepositoryPort = mock(ClientRepositoryPort.class);
        userRepositoryPort = mock(UserRepositoryPort.class);
        outboxEventPort = mock(OutboxEventPort.class);
        invoicePaymentService = new InvoicePaymentService(
                invoiceRepositoryPort, clientRepositoryPort, userRepositoryPort,
                paymentPort, outboxEventPort, "http://localhost:5173");
        chapaWebhookService = new ChapaWebhookService(paymentPort, invoicePaymentService);
    }

    @Test
    void handle_ignoresFailedWebhook() {
        when(paymentPort.parseWebhook(any(), any()))
                .thenReturn(new PaymentPort.WebhookEvent("failed", UUID.randomUUID().toString(), "tx-1", "tx-1"));

        chapaWebhookService.handle("{}", null);

        verify(invoiceRepositoryPort, never()).save(any());
        verify(outboxEventPort, never()).publish(any(), any(), any(), any());
    }

    @Test
    void handle_marksInvoicePaidOnSuccess() {
        UUID userId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Invoice invoice = Invoice.create(
                userId, clientId, "INV-0001", LocalDate.now(), LocalDate.now().plusDays(30), "USD",
                Collections.singletonList(InvoiceItem.create("Work", BigDecimal.ONE, BigDecimal.TEN, 1)),
                null, null, null, null, null);
        invoice.markSent();
        UUID invoiceId = invoice.getId();

        User owner = User.reconstitute(userId, "o@test.com", "h", null, "Owner", "Biz", null, null, null,
                "USD", "CHASECK_TEST", null, com.invoiceflow.domain.model.Plan.FREE,
                true, 3, 7, java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        Client client = Client.create(userId, "Client", "c@test.com", null, null, "USD");

        when(paymentPort.parseWebhook(any(), any()))
                .thenReturn(new PaymentPort.WebhookEvent("charge.success", invoiceId.toString(), "tx-abc", "tx-abc"));
        when(invoiceRepositoryPort.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(invoiceRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientRepositoryPort.findById(clientId)).thenReturn(Optional.of(client));
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(owner));

        chapaWebhookService.handle("{}", "sig");

        verify(invoiceRepositoryPort).save(argThat(i -> i.getStatus() == InvoiceStatus.PAID));
        verify(outboxEventPort, times(2)).publish(eq("Invoice"), eq(invoiceId), anyString(), anyMap());
    }
}
