package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.DownloadInvoicePdfUseCase;
import com.invoiceflow.domain.port.in.PreviewInvoiceUseCase;
import com.invoiceflow.domain.port.in.SendInvoiceUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.EmailPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import com.invoiceflow.domain.port.out.PdfGeneratorPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InvoicePdfAndEmailService implements
        PreviewInvoiceUseCase,
        DownloadInvoicePdfUseCase,
        SendInvoiceUseCase {

    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final PdfGeneratorPort pdfGeneratorPort;
    private final EmailPort emailPort;
    private final String frontendUrl;

    public InvoicePdfAndEmailService(
            InvoiceRepositoryPort invoiceRepositoryPort,
            ClientRepositoryPort clientRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            PdfGeneratorPort pdfGeneratorPort,
            EmailPort emailPort,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.invoiceRepositoryPort = invoiceRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.pdfGeneratorPort = pdfGeneratorPort;
        this.emailPort = emailPort;
        this.frontendUrl = frontendUrl;
    }

    @Override
    @Transactional(readOnly = true)
    public String previewInvoice(PreviewInvoiceUseCase.Query query) {
        Invoice invoice = loadInvoice(query.invoiceId(), query.userId());
        Client client = loadClient(invoice.getClientId(), query.userId());
        User user = loadUser(query.userId());
        return pdfGeneratorPort.generateInvoiceHtml(invoice, client, user);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadPdf(DownloadInvoicePdfUseCase.Query query) {
        Invoice invoice = loadInvoice(query.invoiceId(), query.userId());
        Client client = loadClient(invoice.getClientId(), query.userId());
        User user = loadUser(query.userId());
        return pdfGeneratorPort.generateInvoicePdf(invoice, client, user);
    }

    @Override
    @Transactional
    public Invoice sendInvoice(SendInvoiceUseCase.Command command) {
        Invoice invoice = loadInvoice(command.invoiceId(), command.userId());
        Client client = loadClient(invoice.getClientId(), command.userId());
        User user = loadUser(command.userId());

        // Generate PDF
        byte[] pdfBytes = pdfGeneratorPort.generateInvoicePdf(invoice, client, user);

        invoice.ensurePublicToken();
        String publicLink = frontendUrl + "/inv/" + invoice.getPublicToken();

        String businessName = (user.getBusinessName() != null && !user.getBusinessName().isBlank())
                ? user.getBusinessName() : user.getFullName();
        emailPort.sendInvoiceToClient(client.getEmail(), client.getName(),
                invoice.getInvoiceNumber(), businessName, pdfBytes, publicLink);

        invoice.markSent();
        return invoiceRepositoryPort.save(invoice);
    }

    private Invoice loadInvoice(UUID invoiceId, UUID userId) {
        return invoiceRepositoryPort.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));
    }

    private Client loadClient(UUID clientId, UUID userId) {
        return clientRepositoryPort.findByIdAndUserId(clientId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
    }

    private User loadUser(UUID userId) {
        return userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
