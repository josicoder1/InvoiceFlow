package com.invoiceflow;

import com.invoiceflow.domain.port.out.EmailPort;
import com.invoiceflow.domain.port.out.PaymentPort;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestEmailConfig {

    @Bean
    @Primary
    public EmailPort testEmailPort() {
        return new EmailPort() {
            @Override public void sendInvoiceToClient(String toEmail, String clientName, String invoiceNumber, String ownerBusinessName, byte[] pdfBytes, String publicPaymentLink) {}
            @Override public void sendProposalToClient(String toEmail, String clientName, String proposalTitle, String ownerBusinessName, String publicLink) {}
            @Override public void sendProposalViewedNotification(String ownerEmail, String clientName, String clientCompany, String proposalTitle) {}
            @Override public void sendProposalAcceptedNotification(String ownerEmail, String proposalTitle, String proposalNumber, String clientName, String signedAt) {}
            @Override public void sendProposalDeclinedNotification(String ownerEmail, String proposalTitle, String clientName, String reason) {}
            @Override public void sendPaymentReceiptToClient(String toEmail, String clientName, String invoiceNumber, String businessName, String amount, String currency) {}
            @Override public void sendInvoicePaidNotification(String ownerEmail, String invoiceNumber, String clientName, String amount, String currency) {}
            @Override public void sendOverdueReminderToClient(String toEmail, String clientName, String invoiceNumber, String businessName, String dueDate, String publicLink) {}
        };
    }

    @Bean
    @Primary
    public PaymentPort testPaymentPort() {
        return new PaymentPort() {
            @Override
            public CheckoutSession createCheckoutSession(CheckoutRequest request) {
                return new CheckoutSession("cs_test", "https://checkout.stripe.test/session", "pi_test");
            }

            @Override
            public WebhookEvent parseWebhook(String payload, String signatureHeader) {
                return new WebhookEvent("charge.success", null, null, null);
            }

            @Override
            public boolean verifyTransaction(String txRef, String chapaMerchantKey) {
                return txRef != null && txRef.startsWith("tx-");
            }
        };
    }
}
