package com.invoiceflow.domain.port.out;

public interface EmailPort {
    void sendInvoiceToClient(String toEmail, String clientName, String invoiceNumber,
                              String ownerBusinessName, byte[] pdfBytes, String publicPaymentLink);

    void sendProposalToClient(String toEmail, String clientName, String proposalTitle,
                               String ownerBusinessName, String publicLink);

    void sendProposalViewedNotification(String ownerEmail, String clientName, String clientCompany,
                                         String proposalTitle);

    void sendProposalAcceptedNotification(String ownerEmail, String proposalTitle,
                                           String proposalNumber, String clientName, String signedAt);

    void sendProposalDeclinedNotification(String ownerEmail, String proposalTitle,
                                           String clientName, String reason);

    void sendPaymentReceiptToClient(String toEmail, String clientName, String invoiceNumber,
                                     String businessName, String amount, String currency);

    void sendInvoicePaidNotification(String ownerEmail, String invoiceNumber,
                                      String clientName, String amount, String currency);

    void sendOverdueReminderToClient(String toEmail, String clientName, String invoiceNumber,
                                      String businessName, String dueDate, String publicLink);
}
