package com.invoiceflow.domain.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface GetActivityFeedUseCase {
    List<ActivityEvent> getActivityFeed(Query query);

    record Query(UUID userId) {}

    record ActivityEvent(
            String eventType,
            String description,
            Instant timestamp,
            String linkTarget
    ) {}
}
