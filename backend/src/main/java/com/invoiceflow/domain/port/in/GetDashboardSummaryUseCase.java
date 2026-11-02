package com.invoiceflow.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface GetDashboardSummaryUseCase {
    Result getSummary(Query query);

    record Query(UUID userId) {}

    record Result(
            BigDecimal totalRevenue,
            BigDecimal totalOutstanding,
            BigDecimal totalOverdueThisMonth,
            long activeClientsCount,
            double conversionRate
    ) {}
}
