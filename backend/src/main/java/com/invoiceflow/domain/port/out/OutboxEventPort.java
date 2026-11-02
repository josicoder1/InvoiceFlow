package com.invoiceflow.domain.port.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface OutboxEventPort {
    void publish(String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload);
    List<OutboxEvent> findUnpublished(int limit);
    void markPublished(UUID id);

    record OutboxEvent(UUID id, String aggregateType, UUID aggregateId, String eventType, String payload) {}
}
