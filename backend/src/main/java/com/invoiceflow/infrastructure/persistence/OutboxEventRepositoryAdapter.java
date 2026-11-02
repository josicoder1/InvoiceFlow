package com.invoiceflow.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invoiceflow.domain.port.out.OutboxEventPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OutboxEventRepositoryAdapter implements OutboxEventPort {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final ObjectMapper objectMapper;

    public OutboxEventRepositoryAdapter(OutboxEventJpaRepository outboxEventJpaRepository,
                                        ObjectMapper objectMapper) {
        this.outboxEventJpaRepository = outboxEventJpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void publish(String aggregateType, UUID aggregateId, String eventType, Map<String, Object> payload) {
        OutboxEventJpaEntity entity = new OutboxEventJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setAggregateType(aggregateType);
        entity.setAggregateId(aggregateId);
        entity.setEventType(eventType);
        try {
            entity.setPayload(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload", e);
        }
        entity.setPublished(false);
        entity.setCreatedAt(LocalDateTime.now());
        outboxEventJpaRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OutboxEvent> findUnpublished(int limit) {
        return outboxEventJpaRepository.findUnpublished().stream()
                .limit(limit)
                .map(e -> new OutboxEvent(e.getId(), e.getAggregateType(), e.getAggregateId(),
                        e.getEventType(), e.getPayload()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markPublished(UUID id) {
        outboxEventJpaRepository.findById(id).ifPresent(e -> {
            e.setPublished(true);
            outboxEventJpaRepository.save(e);
        });
    }
}
