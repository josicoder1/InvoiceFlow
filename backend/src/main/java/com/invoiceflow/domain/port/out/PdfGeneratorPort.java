package com.invoiceflow.domain.port.out;

import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.Proposal;
import com.invoiceflow.domain.model.User;

public interface PdfGeneratorPort {
    byte[] generateInvoicePdf(Invoice invoice, Client client, User user);
    String generateInvoiceHtml(Invoice invoice, Client client, User user);
    byte[] generateProposalPdf(Proposal proposal, Client client, User user);
}

