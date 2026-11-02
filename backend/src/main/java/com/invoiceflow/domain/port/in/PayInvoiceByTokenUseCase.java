package com.invoiceflow.domain.port.in;

public interface PayInvoiceByTokenUseCase {
    PayResult createCheckoutSession(String publicToken);

    void confirmPayment(String publicToken, String txRef);

    record PayResult(String checkoutUrl, String sessionId) {}
}
