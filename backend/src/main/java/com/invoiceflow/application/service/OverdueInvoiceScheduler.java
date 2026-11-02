package com.invoiceflow.application.service;

import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.port.in.MarkOverdueInvoicesUseCase;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class OverdueInvoiceScheduler implements MarkOverdueInvoicesUseCase {

    private static final Logger log = LoggerFactory.getLogger(OverdueInvoiceScheduler.class);

    private final InvoiceRepositoryPort invoiceRepositoryPort;

    public OverdueInvoiceScheduler(InvoiceRepositoryPort invoiceRepositoryPort) {
        this.invoiceRepositoryPort = invoiceRepositoryPort;
    }

    /**
     * UC-23: Auto-mark overdue — runs daily at midnight UTC.
     * Finds all SENT invoices whose due date is before today and transitions them to OVERDUE.
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    @Transactional
    public void runDailyOverdueCheck() {
        int count = markOverdueInvoices();
        if (count > 0) {
            log.info("Overdue check complete: {} invoice(s) marked OVERDUE", count);
        }
    }

    @Override
    @Transactional
    public int markOverdueInvoices() {
        LocalDate today = LocalDate.now();
        List<Invoice> overdue = invoiceRepositoryPort.findSentInvoicesPastDueDate(today);

        int count = 0;
        for (Invoice invoice : overdue) {
            try {
                invoice.markOverdue();
                invoiceRepositoryPort.save(invoice);
                count++;
                log.debug("Marked invoice {} as OVERDUE (due: {})", invoice.getInvoiceNumber(), invoice.getDueDate());
            } catch (Exception e) {
                log.error("Failed to mark invoice {} as OVERDUE: {}", invoice.getInvoiceNumber(), e.getMessage());
            }
        }
        return count;
    }
}
