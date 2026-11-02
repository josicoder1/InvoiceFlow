package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.BusinessRuleViolationException;
import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.PayInvoiceByTokenUseCase;
import com.invoiceflow.domain.port.in.ViewInvoiceByTokenUseCase;
import com.invoiceflow.domain.model.InvoiceStatus;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import com.invoiceflow.domain.port.out.OutboxEventPort;
import com.invoiceflow.domain.port.out.PaymentPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
public class InvoicePaymentService implements ViewInvoiceByTokenUseCase, PayInvoiceByTokenUseCase {

    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PaymentPort paymentPort;
    private final OutboxEventPort outboxEventPort;
    private final String frontendUrl;

    public InvoicePaymentService(
            InvoiceRepositoryPort invoiceRepositoryPort,
            ClientRepositoryPort clientRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            PaymentPort paymentPort,
            OutboxEventPort outboxEventPort,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.invoiceRepositoryPort = invoiceRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.paymentPort = paymentPort;
        this.outboxEventPort = outboxEventPort;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public Invoice viewByToken(String publicToken) {
        return invoiceRepositoryPort.findByPublicToken(publicToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    @Override
    @Transactional
    public PayResult createCheckoutSession(String publicToken) {
        Invoice invoice = viewByToken(publicToken);
        if (!invoice.isPayable()) {
            throw new BusinessRuleViolationException("This invoice cannot be paid in its current status");
        }

        User owner = userRepositoryPort.findById(invoice.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice owner not found"));
        if (!owner.isChapaConnected()) {
            throw new BusinessRuleViolationException("Online payments are not enabled for this invoice");
        }

        Client client = clientRepositoryPort.findById(invoice.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        invoice.ensurePublicToken();
        String successUrl = frontendUrl + "/inv/" + publicToken + "/success";
        String cancelUrl = frontendUrl + "/inv/" + publicToken;

        PaymentPort.CheckoutSession session = paymentPort.createCheckoutSession(new PaymentPort.CheckoutRequest(
                invoice.getId().toString(),
                invoice.getInvoiceNumber(),
                invoice.getCurrency(),
                invoice.getTotal(),
                client.getEmail(),
                owner.getChapaMerchantKey(),
                successUrl,
                cancelUrl
        ));

        invoice.recordChapaCheckout(session.sessionId(), session.txRef());
        invoice.updateChapaPaymentLink(session.checkoutUrl());
        invoiceRepositoryPort.save(invoice);

        return new PayResult(session.checkoutUrl(), session.sessionId());
    }

    @Override
    @Transactional
    public void confirmPayment(String publicToken, String txRef) {
        if (txRef == null || txRef.isBlank()) {
            throw new BusinessRuleViolationException("Payment reference is required");
        }

        Invoice invoice = viewByToken(publicToken);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return;
        }

        User owner = userRepositoryPort.findById(invoice.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice owner not found"));
        if (!owner.isChapaConnected()) {
            throw new BusinessRuleViolationException("Online payments are not enabled for this invoice");
        }

        if (!paymentPort.verifyTransaction(txRef, owner.getChapaMerchantKey())) {
            throw new BusinessRuleViolationException("Payment could not be verified. Please wait a moment and refresh.");
        }

        finalizePayment(invoice, txRef, txRef);
    }

    @Transactional
    public void finalizePaymentFromWebhook(UUID invoiceId, String txRef, String sessionId) {
        Invoice invoice = invoiceRepositoryPort.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            return;
        }
        finalizePayment(invoice, txRef, sessionId);
    }

    private void finalizePayment(Invoice invoice, String txRef, String sessionId) {
        invoice.markPaid(LocalDateTime.now());
        invoice.recordChapaCheckout(sessionId, txRef);
        Invoice saved = invoiceRepositoryPort.save(invoice);

        Client client = clientRepositoryPort.findById(saved.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
        User owner = userRepositoryPort.findById(saved.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String businessName = owner.getBusinessName() != null && !owner.getBusinessName().isBlank()
                ? owner.getBusinessName() : owner.getFullName();

        outboxEventPort.publish("Invoice", saved.getId(), "INVOICE_PAYMENT_RECEIPT", Map.of(
                "toEmail", client.getEmail(),
                "clientName", client.getName(),
                "invoiceNumber", saved.getInvoiceNumber(),
                "businessName", businessName,
                "amount", saved.getTotal().toPlainString(),
                "currency", saved.getCurrency()
        ));

        outboxEventPort.publish("Invoice", saved.getId(), "INVOICE_PAID", Map.of(
                "ownerEmail", owner.getEmail(),
                "invoiceNumber", saved.getInvoiceNumber(),
                "clientName", client.getName(),
                "amount", saved.getTotal().toPlainString(),
                "currency", saved.getCurrency()
        ));
    }
}
