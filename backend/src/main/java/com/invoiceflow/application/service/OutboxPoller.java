package com.invoiceflow.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceflow.domain.port.out.EmailPort;
import com.invoiceflow.domain.port.out.OutboxEventPort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OutboxPoller {

    private final OutboxEventPort outboxEventPort;
    private final EmailPort emailPort;
    private final ObjectMapper objectMapper;

    public OutboxPoller(OutboxEventPort outboxEventPort, EmailPort emailPort, ObjectMapper objectMapper) {
        this.outboxEventPort = outboxEventPort;
        this.emailPort = emailPort;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void poll() {
        for (OutboxEventPort.OutboxEvent event : outboxEventPort.findUnpublished(20)) {
            try {
                Map<String, String> payload = objectMapper.readValue(
                        event.payload(), new TypeReference<>() {});

                switch (event.eventType()) {
                    case "PROPOSAL_SENT" -> emailPort.sendProposalToClient(
                            payload.get("toEmail"),
                            payload.get("clientName"),
                            payload.get("proposalTitle"),
                            payload.get("businessName"),
                            payload.get("publicLink"));
                    case "PROPOSAL_VIEWED" -> emailPort.sendProposalViewedNotification(
                            payload.get("ownerEmail"),
                            payload.get("clientName"),
                            payload.getOrDefault("clientCompany", ""),
                            payload.get("proposalTitle"));
                    case "PROPOSAL_ACCEPTED" -> emailPort.sendProposalAcceptedNotification(
                            payload.get("ownerEmail"),
                            payload.get("proposalTitle"),
                            payload.get("proposalNumber"),
                            payload.get("clientName"),
                            payload.get("signedAt"));
                    case "PROPOSAL_DECLINED" -> emailPort.sendProposalDeclinedNotification(
                            payload.get("ownerEmail"),
                            payload.get("proposalTitle"),
                            payload.get("clientName"),
                            payload.getOrDefault("reason", ""));
                    case "INVOICE_PAYMENT_RECEIPT" -> emailPort.sendPaymentReceiptToClient(
                            payload.get("toEmail"),
                            payload.get("clientName"),
                            payload.get("invoiceNumber"),
                            payload.get("businessName"),
                            payload.get("amount"),
                            payload.get("currency"));
                    case "INVOICE_PAID" -> emailPort.sendInvoicePaidNotification(
                            payload.get("ownerEmail"),
                            payload.get("invoiceNumber"),
                            payload.get("clientName"),
                            payload.get("amount"),
                            payload.get("currency"));
                    case "INVOICE_OVERDUE_REMINDER" -> emailPort.sendOverdueReminderToClient(
                            payload.get("toEmail"),
                            payload.get("clientName"),
                            payload.get("invoiceNumber"),
                            payload.get("businessName"),
                            payload.get("dueDate"),
                            payload.get("publicLink"));
                    default -> { /* ignore unknown */ }
                }
                outboxEventPort.markPublished(event.id());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
