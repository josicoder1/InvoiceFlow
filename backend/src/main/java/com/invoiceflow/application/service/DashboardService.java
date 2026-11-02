package com.invoiceflow.application.service;

import com.invoiceflow.domain.model.Client;
import com.invoiceflow.domain.model.Invoice;
import com.invoiceflow.domain.model.InvoiceStatus;
import com.invoiceflow.domain.model.Proposal;
import com.invoiceflow.domain.model.ProposalStatus;
import com.invoiceflow.domain.port.in.ExportInvoicesCsvUseCase;
import com.invoiceflow.domain.port.in.GetActivityFeedUseCase;
import com.invoiceflow.domain.port.in.GetDashboardSummaryUseCase;
import com.invoiceflow.domain.port.in.GetRevenueChartUseCase;
import com.invoiceflow.domain.port.out.ClientRepositoryPort;
import com.invoiceflow.domain.port.out.InvoiceRepositoryPort;
import com.invoiceflow.domain.port.out.ProposalRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardService implements
        GetDashboardSummaryUseCase,
        GetRevenueChartUseCase,
        GetActivityFeedUseCase,
        ExportInvoicesCsvUseCase {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");

    private final InvoiceRepositoryPort invoiceRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final ProposalRepositoryPort proposalRepositoryPort;

    public DashboardService(
            InvoiceRepositoryPort invoiceRepositoryPort,
            ClientRepositoryPort clientRepositoryPort,
            ProposalRepositoryPort proposalRepositoryPort) {
        this.invoiceRepositoryPort = invoiceRepositoryPort;
        this.clientRepositoryPort = clientRepositoryPort;
        this.proposalRepositoryPort = proposalRepositoryPort;
    }

    @Override
    @Transactional(readOnly = true)
    public GetDashboardSummaryUseCase.Result getSummary(GetDashboardSummaryUseCase.Query query) {
        UUID userId = query.userId();

        BigDecimal totalRevenue = invoiceRepositoryPort.sumTotalByUserIdAndStatus(userId, InvoiceStatus.PAID);
        BigDecimal sentTotal = invoiceRepositoryPort.sumTotalByUserIdAndStatus(userId, InvoiceStatus.SENT);
        BigDecimal overdueTotal = invoiceRepositoryPort.sumTotalByUserIdAndStatus(userId, InvoiceStatus.OVERDUE);
        BigDecimal totalOutstanding = sentTotal.add(overdueTotal);

        YearMonth currentMonth = YearMonth.now();
        BigDecimal totalOverdueThisMonth = invoiceRepositoryPort.sumTotalByUserIdAndStatusAndDueDateBetween(
                userId, InvoiceStatus.OVERDUE,
                currentMonth.atDay(1), currentMonth.atEndOfMonth());

        long activeClientsCount = clientRepositoryPort.countByUserId(userId);

        long accepted = proposalRepositoryPort.countByUserIdAndStatusIn(
                userId, List.of(ProposalStatus.ACCEPTED));
        long denominator = proposalRepositoryPort.countByUserIdAndStatusIn(
                userId, List.of(ProposalStatus.SENT, ProposalStatus.VIEWED,
                        ProposalStatus.ACCEPTED, ProposalStatus.DECLINED));

        double conversionRate = denominator == 0 ? 0.0
                : BigDecimal.valueOf(accepted)
                        .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();

        return new GetDashboardSummaryUseCase.Result(totalRevenue, totalOutstanding, totalOverdueThisMonth,
                activeClientsCount, conversionRate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetRevenueChartUseCase.DataPoint> getRevenueChart(GetRevenueChartUseCase.Query query) {
        UUID userId = query.userId();
        YearMonth currentMonth = YearMonth.now();
        YearMonth startMonth = currentMonth.minusMonths(11);

        LocalDateTime rangeStart = startMonth.atDay(1).atStartOfDay();
        LocalDateTime rangeEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay();

        Map<YearMonth, BigDecimal> revenueByMonth = invoiceRepositoryPort
                .sumPaidRevenueByMonth(userId, rangeStart, rangeEnd)
                .stream()
                .collect(Collectors.toMap(
                        r -> YearMonth.of(r.year(), r.month()),
                        InvoiceRepositoryPort.MonthlyRevenue::revenue,
                        BigDecimal::add));

        List<GetRevenueChartUseCase.DataPoint> chart = new ArrayList<>();
        for (YearMonth month = startMonth; !month.isAfter(currentMonth); month = month.plusMonths(1)) {
            BigDecimal revenue = revenueByMonth.getOrDefault(month, BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            chart.add(new GetRevenueChartUseCase.DataPoint(month.format(MONTH_FORMAT), revenue));
        }
        return chart;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GetActivityFeedUseCase.ActivityEvent> getActivityFeed(GetActivityFeedUseCase.Query query) {
        UUID userId = query.userId();

        List<Invoice> recentInvoices = invoiceRepositoryPort.findRecentByUserId(userId, 20);
        List<Proposal> recentProposals = proposalRepositoryPort.findRecentByUserId(userId, 20);

        Map<UUID, String> clientNames = loadClientNames(userId, recentInvoices, recentProposals);

        List<GetActivityFeedUseCase.ActivityEvent> events = new ArrayList<>();
        for (Invoice invoice : recentInvoices) {
            String clientName = clientNames.getOrDefault(invoice.getClientId(), "Unknown client");
            events.add(mapInvoiceEvent(invoice, clientName));
        }
        for (Proposal proposal : recentProposals) {
            String clientName = clientNames.getOrDefault(proposal.getClientId(), "Unknown client");
            events.add(mapProposalEvent(proposal, clientName));
        }

        return events.stream()
                .sorted(Comparator.comparing(GetActivityFeedUseCase.ActivityEvent::timestamp).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(ExportInvoicesCsvUseCase.Query query) {
        List<Invoice> invoices = invoiceRepositoryPort.findAllByUserId(query.userId());
        Map<UUID, String> clientNames = new HashMap<>();
        for (Invoice invoice : invoices) {
            clientRepositoryPort.findById(invoice.getClientId())
                    .ifPresent(c -> clientNames.put(c.getId(), c.getName()));
        }

        StringBuilder csv = new StringBuilder();
        csv.append("Invoice Number,Client,Status,Issue Date,Due Date,Total,Paid At\n");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (Invoice invoice : invoices) {
            String clientName = escapeCsv(clientNames.getOrDefault(invoice.getClientId(), "Unknown"));
            String paidAt = invoice.getPaidAt() != null
                    ? invoice.getPaidAt().format(dateTimeFormatter)
                    : "";
            csv.append(escapeCsv(invoice.getInvoiceNumber())).append(',')
                    .append(clientName).append(',')
                    .append(invoice.getStatus().name()).append(',')
                    .append(invoice.getIssueDate().format(dateFormatter)).append(',')
                    .append(invoice.getDueDate().format(dateFormatter)).append(',')
                    .append(invoice.getTotal().setScale(2, RoundingMode.HALF_UP)).append(',')
                    .append(paidAt).append('\n');
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private Map<UUID, String> loadClientNames(
            UUID userId, List<Invoice> invoices, List<Proposal> proposals) {
        Map<UUID, String> names = new HashMap<>();
        for (Invoice invoice : invoices) {
            resolveClientName(userId, invoice.getClientId(), names);
        }
        for (Proposal proposal : proposals) {
            resolveClientName(userId, proposal.getClientId(), names);
        }
        return names;
    }

    private void resolveClientName(UUID userId, UUID clientId, Map<UUID, String> names) {
        if (names.containsKey(clientId)) return;
        clientRepositoryPort.findByIdAndUserId(clientId, userId)
                .ifPresent(c -> names.put(clientId, c.getName()));
    }

    private GetActivityFeedUseCase.ActivityEvent mapInvoiceEvent(Invoice invoice, String clientName) {
        Instant timestamp = resolveInvoiceTimestamp(invoice);
        String linkTarget = "/invoices/" + invoice.getId();
        return switch (invoice.getStatus()) {
            case DRAFT -> new GetActivityFeedUseCase.ActivityEvent("INVOICE_CREATED",
                    "Invoice " + invoice.getInvoiceNumber() + " created", timestamp, linkTarget);
            case SENT -> new GetActivityFeedUseCase.ActivityEvent("INVOICE_SENT",
                    "Invoice " + invoice.getInvoiceNumber() + " sent to " + clientName, timestamp, linkTarget);
            case PAID -> new GetActivityFeedUseCase.ActivityEvent("INVOICE_PAID",
                    "Invoice " + invoice.getInvoiceNumber() + " paid by " + clientName, timestamp, linkTarget);
            case OVERDUE -> new GetActivityFeedUseCase.ActivityEvent("INVOICE_OVERDUE",
                    "Invoice " + invoice.getInvoiceNumber() + " is overdue", timestamp, linkTarget);
            case CANCELLED -> new GetActivityFeedUseCase.ActivityEvent("INVOICE_CANCELLED",
                    "Invoice " + invoice.getInvoiceNumber() + " cancelled", timestamp, linkTarget);
        };
    }

    private GetActivityFeedUseCase.ActivityEvent mapProposalEvent(Proposal proposal, String clientName) {
        Instant timestamp = resolveProposalTimestamp(proposal);
        String linkTarget = "/proposals/" + proposal.getId();
        return switch (proposal.getStatus()) {
            case DRAFT -> new GetActivityFeedUseCase.ActivityEvent("PROPOSAL_CREATED",
                    "Proposal \"" + proposal.getTitle() + "\" created", timestamp, linkTarget);
            case SENT -> new GetActivityFeedUseCase.ActivityEvent("PROPOSAL_SENT",
                    "Proposal \"" + proposal.getTitle() + "\" sent to " + clientName, timestamp, linkTarget);
            case VIEWED -> new GetActivityFeedUseCase.ActivityEvent("PROPOSAL_VIEWED",
                    "Proposal \"" + proposal.getTitle() + "\" viewed by " + clientName, timestamp, linkTarget);
            case ACCEPTED -> new GetActivityFeedUseCase.ActivityEvent("PROPOSAL_ACCEPTED",
                    "Proposal \"" + proposal.getTitle() + "\" accepted by " + clientName, timestamp, linkTarget);
            case DECLINED -> new GetActivityFeedUseCase.ActivityEvent("PROPOSAL_DECLINED",
                    "Proposal \"" + proposal.getTitle() + "\" declined by " + clientName, timestamp, linkTarget);
        };
    }

    private Instant resolveInvoiceTimestamp(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID && invoice.getPaidAt() != null) {
            return invoice.getPaidAt().atZone(java.time.ZoneOffset.UTC).toInstant();
        }
        return invoice.getUpdatedAt().atZone(java.time.ZoneOffset.UTC).toInstant();
    }

    private Instant resolveProposalTimestamp(Proposal proposal) {
        if (proposal.getStatus() == ProposalStatus.VIEWED && proposal.getViewedAt() != null) {
            return proposal.getViewedAt().atZone(java.time.ZoneOffset.UTC).toInstant();
        }
        if (proposal.getStatus() == ProposalStatus.ACCEPTED && proposal.getSignedAt() != null) {
            return proposal.getSignedAt().atZone(java.time.ZoneOffset.UTC).toInstant();
        }
        return proposal.getUpdatedAt().atZone(java.time.ZoneOffset.UTC).toInstant();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
