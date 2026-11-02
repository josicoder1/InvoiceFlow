package com.invoiceflow.infrastructure.email;

import com.invoiceflow.domain.port.out.EmailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SpringMailEmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;

    public SpringMailEmailAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendInvoiceToClient(String toEmail, String clientName, String invoiceNumber,
                                     String ownerBusinessName, byte[] pdfBytes, String publicPaymentLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Invoice " + invoiceNumber + " from " + ownerBusinessName);
            helper.setText(buildInvoiceEmailHtml(clientName, invoiceNumber, ownerBusinessName, publicPaymentLink), true);
            helper.addAttachment(invoiceNumber + ".pdf",
                    new org.springframework.core.io.ByteArrayResource(pdfBytes), "application/pdf");
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send invoice email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendProposalToClient(String toEmail, String clientName, String proposalTitle,
                                      String ownerBusinessName, String publicLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Proposal from " + ownerBusinessName + ": " + proposalTitle);
            helper.setText(buildProposalEmailHtml(clientName, proposalTitle, ownerBusinessName, publicLink), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send proposal email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendProposalViewedNotification(String ownerEmail, String clientName, String clientCompany,
                                              String proposalTitle) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(ownerEmail);
            helper.setSubject("Your proposal was viewed by " + clientName);
            String companyPart = (clientCompany != null && !clientCompany.isBlank())
                    ? " from " + clientCompany : "";
            helper.setText("""
                    <!DOCTYPE html><html><body style="font-family:Helvetica,Arial,sans-serif;padding:24px;">
                    <p>%s%s just opened your proposal <strong>%s</strong>.</p>
                    <p style="color:#6b7280;">Log in to follow up.</p>
                    </body></html>
                    """.formatted(clientName, companyPart, proposalTitle), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send proposal viewed notification: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendProposalAcceptedNotification(String ownerEmail, String proposalTitle,
                                                  String proposalNumber, String clientName, String signedAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(ownerEmail);
            helper.setSubject("Proposal accepted: " + proposalTitle);
            helper.setText("""
                    <!DOCTYPE html><html><body style="font-family:Helvetica,Arial,sans-serif;padding:24px;">
                    <p><strong>%s</strong> accepted your proposal <strong>%s</strong> (%s).</p>
                    <p style="color:#6b7280;">Signed at: %s</p>
                    </body></html>
                    """.formatted(clientName, proposalTitle, proposalNumber, signedAt), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send proposal accepted notification: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendProposalDeclinedNotification(String ownerEmail, String proposalTitle,
                                                  String clientName, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(ownerEmail);
            helper.setSubject("Proposal declined: " + proposalTitle);
            String reasonText = (reason != null && !reason.isBlank())
                    ? "<p>Reason: " + reason + "</p>" : "";
            helper.setText("""
                    <!DOCTYPE html><html><body style="font-family:Helvetica,Arial,sans-serif;padding:24px;">
                    <p><strong>%s</strong> declined your proposal <strong>%s</strong>.</p>
                    %s
                    </body></html>
                    """.formatted(clientName, proposalTitle, reasonText), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send proposal declined notification: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPaymentReceiptToClient(String toEmail, String clientName, String invoiceNumber,
                                            String businessName, String amount, String currency) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Payment receipt for " + invoiceNumber);
            helper.setText("""
                    <!DOCTYPE html><html><body style="font-family:Helvetica,Arial,sans-serif;padding:24px;">
                    <p>Hi <strong>%s</strong>,</p>
                    <p>Thank you — your payment of <strong>%s %s</strong> for invoice <strong>%s</strong> from <strong>%s</strong> was received.</p>
                    </body></html>
                    """.formatted(clientName, currency, amount, invoiceNumber, businessName), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send payment receipt: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendInvoicePaidNotification(String ownerEmail, String invoiceNumber,
                                             String clientName, String amount, String currency) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(ownerEmail);
            helper.setSubject("Invoice paid: " + invoiceNumber);
            helper.setText("""
                    <!DOCTYPE html><html><body style="font-family:Helvetica,Arial,sans-serif;padding:24px;">
                    <p><strong>%s</strong> paid invoice <strong>%s</strong> (%s %s).</p>
                    <p style="color:#6b7280;">Log in to view details.</p>
                    </body></html>
                    """.formatted(clientName, invoiceNumber, currency, amount), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send invoice paid notification: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendOverdueReminderToClient(String toEmail, String clientName, String invoiceNumber,
                                               String businessName, String dueDate, String publicLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Friendly reminder: Invoice " + invoiceNumber + " from " + businessName);
            String payButton = publicLink != null
                    ? "<div style=\"text-align:center;margin:24px 0;\"><a href=\"" + publicLink + "\" style=\"background:#4f46e5;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:bold;\">View & Pay Invoice</a></div>"
                    : "";
            helper.setText("""
                    <!DOCTYPE html><html><body style="font-family:Helvetica,Arial,sans-serif;padding:24px;">
                    <p>Hi <strong>%s</strong>,</p>
                    <p>This is a friendly reminder that invoice <strong>%s</strong> from <strong>%s</strong> was due on <strong>%s</strong>.</p>
                    %s
                    <p style="color:#6b7280;font-size:13px;">If you have already paid, please disregard this message.</p>
                    </body></html>
                    """.formatted(clientName, invoiceNumber, businessName, dueDate, payButton), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send overdue reminder: " + e.getMessage(), e);
        }
    }

    private String buildInvoiceEmailHtml(String clientName, String invoiceNumber, String businessName, String publicPaymentLink) {
        String paySection = publicPaymentLink != null
                ? """
                  <div style="text-align:center;margin:28px 0;">
                    <a href="%s" style="background:#4f46e5;color:#fff;padding:12px 28px;border-radius:8px;text-decoration:none;font-weight:bold;font-size:15px;">View & Pay Online →</a>
                  </div>
                  <p style="font-size:13px;color:#9ca3af;">Or copy: <a href="%s" style="color:#4f46e5;">%s</a></p>
                  """.formatted(publicPaymentLink, publicPaymentLink, publicPaymentLink)
                : "";
        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"></head>
                <body style="font-family:Helvetica,Arial,sans-serif;background:#f4f4f8;padding:32px;">
                  <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;">
                    <div style="background:#4f46e5;padding:28px 32px;">
                      <h1 style="margin:0;color:#fff;font-size:20px;">Invoice %s</h1>
                      <p style="margin:6px 0 0;color:#c7d2fe;font-size:14px;">From %s</p>
                    </div>
                    <div style="padding:28px 32px;">
                      <p style="font-size:15px;color:#374151;">Hi <strong>%s</strong>,</p>
                      <p style="font-size:14px;color:#6b7280;line-height:1.6;">
                        Please find your invoice <strong>%s</strong> attached as a PDF.
                      </p>
                      %s
                      <p style="font-size:14px;color:#374151;margin-top:24px;">Thank you for your business!<br><strong>%s</strong></p>
                    </div>
                    <div style="background:#f9fafb;padding:16px 32px;text-align:center;">
                      <p style="font-size:11px;color:#9ca3af;margin:0;">Sent via InvoiceFlow</p>
                    </div>
                  </div>
                </body></html>
                """.formatted(invoiceNumber, businessName, clientName, invoiceNumber, paySection, businessName);
    }

    private String buildProposalEmailHtml(String clientName, String proposalTitle,
                                           String businessName, String publicLink) {
        return """
                <!DOCTYPE html>
                <html><head><meta charset="UTF-8"></head>
                <body style="font-family:Helvetica,Arial,sans-serif;background:#f4f4f8;padding:32px;">
                  <div style="max-width:560px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;">
                    <div style="background:#4f46e5;padding:28px 32px;">
                      <h1 style="margin:0;color:#fff;font-size:20px;">Proposal: %s</h1>
                      <p style="margin:6px 0 0;color:#c7d2fe;font-size:14px;">From %s</p>
                    </div>
                    <div style="padding:28px 32px;">
                      <p style="font-size:15px;color:#374151;">Hi <strong>%s</strong>,</p>
                      <p style="font-size:14px;color:#6b7280;line-height:1.6;">
                        Please review the proposal <strong>%s</strong> using the link below.
                      </p>
                      <div style="text-align:center;margin:28px 0;">
                        <a href="%s" style="background:#4f46e5;color:#fff;padding:12px 28px;border-radius:8px;text-decoration:none;font-weight:bold;font-size:15px;">View Proposal →</a>
                      </div>
                      <p style="font-size:13px;color:#9ca3af;">Or copy: <a href="%s" style="color:#4f46e5;">%s</a></p>
                    </div>
                    <div style="background:#f9fafb;padding:16px 32px;text-align:center;">
                      <p style="font-size:11px;color:#9ca3af;margin:0;">Sent via InvoiceFlow</p>
                    </div>
                  </div>
                </body></html>
                """.formatted(proposalTitle, businessName, clientName, proposalTitle,
                              publicLink, publicLink, publicLink);
    }
}
