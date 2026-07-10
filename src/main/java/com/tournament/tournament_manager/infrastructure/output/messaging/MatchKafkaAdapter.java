package com.tournament.tournament_manager.infrastructure.output.messaging;

import com.tournament.tournament_manager.config.kafka.KafkaConfig;
import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.port.out.match.PublishMatchEventPort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Adapter Kafka implémentant la publication des événements de fin de match.
 * Publie sur le topic {@code match-finished} via {@link KafkaTemplate}.
 */
@Component
public class MatchKafkaAdapter implements PublishMatchEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public MatchKafkaAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishMatchFinished(MatchFinishedEvent event) {
        kafkaTemplate.send(KafkaConfig.MATCH_FINISHED_TOPIC, event);
    }
}