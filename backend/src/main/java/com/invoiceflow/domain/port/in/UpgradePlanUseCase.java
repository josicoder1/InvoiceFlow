package com.invoiceflow.domain.port.in;

import com.invoiceflow.domain.model.Plan;

import java.util.UUID;

public interface UpgradePlanUseCase {
    GetSubscriptionStatusUseCase.Result upgrade(Command command);

    record Command(UUID userId, Plan plan) {}
}
