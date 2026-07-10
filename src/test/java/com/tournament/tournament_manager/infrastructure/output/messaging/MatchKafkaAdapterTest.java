package com.tournament.tournament_manager.infrastructure.output.messaging;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static com.tournament.tournament_manager.config.kafka.KafkaConfig.MATCH_FINISHED_TOPIC;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MatchKafkaAdapterTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private MatchKafkaAdapter matchKafkaAdapter;

    @Test
    void publishMatchFinished_shouldSendToKafkaTopic() {
        MatchFinishedEvent event = new MatchFinishedEvent(1L);

        matchKafkaAdapter.publishMatchFinished(event);

        verify(kafkaTemplate, times(1)).send(MATCH_FINISHED_TOPIC, event);
    }
}