package com.invoiceflow.domain.port.in;
 
public interface HandleChapaWebhookUseCase {
    void handle(String payload, String signatureHeader);
}
