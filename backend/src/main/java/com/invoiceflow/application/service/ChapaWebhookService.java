package com.invoiceflow.application.service;
 
import com.invoiceflow.domain.port.in.HandleChapaWebhookUseCase;
import com.invoiceflow.domain.port.out.PaymentPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.util.UUID;
 
@Service
public class ChapaWebhookService implements HandleChapaWebhookUseCase {
 
    private final PaymentPort paymentPort;
    private final InvoicePaymentService invoicePaymentService;
 
    public ChapaWebhookService(PaymentPort paymentPort, InvoicePaymentService invoicePaymentService) {
        this.paymentPort = paymentPort;
        this.invoicePaymentService = invoicePaymentService;
    }
 
    @Override
    @Transactional
    public void handle(String payload, String signatureHeader) {
        PaymentPort.WebhookEvent event = paymentPort.parseWebhook(payload, signatureHeader);
        if (event.invoiceId() == null || "failed".equalsIgnoreCase(event.type())) {
            return;
        }
 
        UUID invoiceId = UUID.fromString(event.invoiceId());
        invoicePaymentService.finalizePaymentFromWebhook(invoiceId, event.txRef(), event.sessionId());
    }
}
