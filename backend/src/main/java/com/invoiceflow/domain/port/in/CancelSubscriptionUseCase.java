package com.invoiceflow.domain.port.in;

import java.util.UUID;

public interface CancelSubscriptionUseCase {
    GetSubscriptionStatusUseCase.Result cancel(Command command);

    record Command(UUID userId) {}
}
