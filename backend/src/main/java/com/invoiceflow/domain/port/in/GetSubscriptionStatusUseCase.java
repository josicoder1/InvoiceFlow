package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Plan;

import java.util.UUID;

public interface GetSubscriptionStatusUseCase {
    Result getStatus(Query query);

    record Query(UUID userId) {}

    record Result(Plan plan, int invoicesThisMonth, long totalClients) {}
}
