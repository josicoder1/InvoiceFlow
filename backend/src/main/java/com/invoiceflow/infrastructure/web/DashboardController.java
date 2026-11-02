package com.invoiceflow.infrastructure.web;

import com.invoiceflow.domain.model.User;
import com.invoiceflow.domain.port.in.GetActivityFeedUseCase;
import com.invoiceflow.domain.port.in.GetDashboardSummaryUseCase;
import com.invoiceflow.domain.port.in.GetRevenueChartUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final GetDashboardSummaryUseCase getDashboardSummaryUseCase;
    private final GetRevenueChartUseCase getRevenueChartUseCase;
    private final GetActivityFeedUseCase getActivityFeedUseCase;

    public DashboardController(
            GetDashboardSummaryUseCase getDashboardSummaryUseCase,
            GetRevenueChartUseCase getRevenueChartUseCase,
            GetActivityFeedUseCase getActivityFeedUseCase) {
        this.getDashboardSummaryUseCase = getDashboardSummaryUseCase;
        this.getRevenueChartUseCase = getRevenueChartUseCase;
        this.getActivityFeedUseCase = getActivityFeedUseCase;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDto> getSummary(@AuthenticationPrincipal User user) {
        GetDashboardSummaryUseCase.Result result = getDashboardSummaryUseCase.getSummary(
                new GetDashboardSummaryUseCase.Query(user.getId()));
        return ResponseEntity.ok(DashboardSummaryDto.from(result));
    }

    @GetMapping("/revenue-chart")
    public ResponseEntity<List<RevenueDataPointDto>> getRevenueChart(@AuthenticationPrincipal User user) {
        List<GetRevenueChartUseCase.DataPoint> data = getRevenueChartUseCase.getRevenueChart(
                new GetRevenueChartUseCase.Query(user.getId()));
        return ResponseEntity.ok(data.stream().map(RevenueDataPointDto::from).toList());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<ActivityEventDto>> getActivity(@AuthenticationPrincipal User user) {
        List<GetActivityFeedUseCase.ActivityEvent> events = getActivityFeedUseCase.getActivityFeed(
                new GetActivityFeedUseCase.Query(user.getId()));
        return ResponseEntity.ok(events.stream().map(ActivityEventDto::from).toList());
    }

    public record DashboardSummaryDto(
            BigDecimal totalRevenue,
            BigDecimal totalOutstanding,
            BigDecimal totalOverdueThisMonth,
            long activeClientsCount,
            double conversionRate
    ) {
        static DashboardSummaryDto from(GetDashboardSummaryUseCase.Result result) {
            return new DashboardSummaryDto(
                    result.totalRevenue(),
                    result.totalOutstanding(),
                    result.totalOverdueThisMonth(),
                    result.activeClientsCount(),
                    result.conversionRate());
        }
    }

    public record RevenueDataPointDto(String month, BigDecimal revenue) {
        static RevenueDataPointDto from(GetRevenueChartUseCase.DataPoint point) {
            return new RevenueDataPointDto(point.month(), point.revenue());
        }
    }

    public record ActivityEventDto(
            String eventType,
            String description,
            Instant timestamp,
            String linkTarget
    ) {
        static ActivityEventDto from(GetActivityFeedUseCase.ActivityEvent event) {
            return new ActivityEventDto(
                    event.eventType(),
                    event.description(),
                    event.timestamp(),
                    event.linkTarget());
        }
    }
}
