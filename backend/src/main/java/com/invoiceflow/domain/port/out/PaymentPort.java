package com.invoiceflow.domain.port.out;

import java.math.BigDecimal;

public interface PaymentPort {

    CheckoutSession createCheckoutSession(CheckoutRequest request);

    WebhookEvent parseWebhook(String payload, String signatureHeader);

    boolean verifyTransaction(String txRef, String chapaMerchantKey);

    record CheckoutRequest(
            String invoiceId,
            String invoiceNumber,
            String currency,
            BigDecimal amount,
            String clientEmail,
            String chapaMerchantKey,
            String successUrl,
            String cancelUrl
    ) {}

    record CheckoutSession(String sessionId, String checkoutUrl, String txRef) {}

    record WebhookEvent(String type, String invoiceId, String sessionId, String txRef) {}
}
