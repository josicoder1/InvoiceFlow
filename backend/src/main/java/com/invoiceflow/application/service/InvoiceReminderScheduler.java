package com.invoiceflow.application.service;

import com.invoiceflow.domain.exception.ResourceNotFoundException;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceStatus;
import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.UpdateNotificationSettingsUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import com.invoiceflow.domain.port.out.OutboxEventPort;
import com.invoiceflow.domain.port.out.UserRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class InvoiceReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvoiceReminderScheduler.class);

    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final OutboxEventPort outboxEventPort;
    private final String frontendUrl;

    public InvoiceReminderScheduler(
            InvoiceRepositoryPort invoiceRepositoryPort,
            UserRepositoryPort userRepositoryPort,
            ClientRepositoryPort clientRepositoryPort,
            OutboxEventPort outboxEventPort,
            @Value("${app.frontend-url}") String frontendUrl) {
        this.invoiceRepositoryPort = invoiceRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.outboxEventPort = outboxEventPort;
        this.frontendUrl = frontendUrl;
    }

    @Scheduled(cron = "0 0 9 * * *", zone = "UTC")
    @Transactional
    public void sendDailyReminders() {
        LocalDate today = LocalDate.now();
        List<Invoice> candidates = invoiceRepositoryPort.findInvoicesNeedingReminder(today);
        int sent = 0;

        for (Invoice invoice : candidates) {
            try {
                User owner = userRepositoryPort.findById(invoice.getUserId()).orElse(null);
                if (owner == null || !owner.isRemindersEnabled()) continue;

                boolean beforeDue = invoice.getStatus() == InvoiceStatus.SENT
                        && today.equals(invoice.getDueDate().minusDays(owner.getReminderDaysBeforeDue()));
                boolean afterDue = invoice.getDueDate().isBefore(today)
                        && (today.equals(invoice.getDueDate().plusDays(owner.getReminderDaysAfterDue()))
                        || invoice.getStatus() == InvoiceStatus.OVERDUE);

                if (!beforeDue && !afterDue) continue;

                var client = clientRepositoryPort.findById(invoice.getClientId()).orElse(null);
                if (client == null) continue;

                invoice.ensurePublicToken();
                String publicLink = frontendUrl + "/inv/" + invoice.getPublicToken();
                String businessName = owner.getBusinessName() != null && !owner.getBusinessName().isBlank()
                        ? owner.getBusinessName() : owner.getFullName();

                outboxEventPort.publish("Invoice", invoice.getId(), "INVOICE_OVERDUE_REMINDER", Map.of(
                        "toEmail", client.getEmail(),
                        "clientName", client.getName(),
                        "invoiceNumber", invoice.getInvoiceNumber(),
                        "businessName", businessName,
                        "dueDate", invoice.getDueDate().toString(),
                        "publicLink", publicLink
                ));

                invoice.markReminderSent();
                invoiceRepositoryPort.save(invoice);
                sent++;
            } catch (Exception e) {
                log.error("Failed to queue reminder for invoice {}: {}", invoice.getInvoiceNumber(), e.getMessage());
            }
        }

        if (sent > 0) {
            log.info("Queued {} invoice reminder(s)", sent);
        }
    }
}
