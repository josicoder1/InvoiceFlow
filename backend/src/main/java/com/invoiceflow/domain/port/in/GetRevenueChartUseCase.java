package com.invoiceflow.domain.port.in;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface GetRevenueChartUseCase {
    List<DataPoint> getRevenueChart(Query query);

    record Query(UUID userId) {}

    record DataPoint(String month, BigDecimal revenue) {}
}
