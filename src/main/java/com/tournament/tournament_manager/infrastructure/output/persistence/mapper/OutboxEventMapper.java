package com.tournament.tournament_manager.infrastructure.output.persistence.mapper;

import com.tournament.tournament_manager.domain.model.OutboxEvent;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.OutboxEventEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxEventMapper {

    public OutboxEvent toDomain(OutboxEventEntity entity) {
        if (entity == null) {
            return null;
        }
        OutboxEvent event = new OutboxEvent();
        event.setId(entity.getId());
        event.setTopic(entity.getTopic());
        event.setPartitionKey(entity.getPartitionKey());
        event.setEventType(entity.getEventType());
        event.setPayload(entity.getPayload());
        event.setCreatedAt(entity.getCreatedAt());
        event.setPublishedAt(entity.getPublishedAt());
        return event;
    }

    public OutboxEventEntity toNewEntity(OutboxEvent event) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setTopic(event.getTopic());
        entity.setPartitionKey(event.getPartitionKey());
        entity.setEventType(event.getEventType());
        entity.setPayload(event.getPayload());
        return entity;
    }
}
