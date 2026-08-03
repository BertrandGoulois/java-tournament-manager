package com.tournament.tournament_manager.infrastructure.output.messaging;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.OutboxEvent;
import com.tournament.tournament_manager.domain.port.out.match.PublishMatchEventPort;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.OutboxEventRepository;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Adapter implémentant la publication des événements de fin de match via le pattern
 * Transactional Outbox (voir {@code OutboxPublisherService} pour la publication Kafka
 * réelle, faite en dehors de toute transaction métier).
 *
 * <p>Écrit une ligne {@link OutboxEvent} dans la transaction en cours (celle de
 * {@code RecordMatchResultService}) plutôt que d'appeler Kafka directement : la
 * publication ne peut alors plus être en avance ou en retard sur le commit de la
 * transaction métier, puisqu'elle n'a lieu qu'après coup, une fois l'écriture en base
 * garantie durable.
 */
@Component
public class MatchKafkaAdapter implements PublishMatchEventPort {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public MatchKafkaAdapter(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishMatchFinished(MatchFinishedEvent event, Long partitionKey) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setTopic(KafkaConfig.MATCH_FINISHED_TOPIC);
        outboxEvent.setPartitionKey(String.valueOf(partitionKey));
        outboxEvent.setEventType(MatchFinishedEvent.class.getSimpleName());
        outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        outboxEventRepository.save(outboxEvent);
    }
}
