package com.tournament.tournament_manager.infrastructure.output.messaging;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.OutboxEvent;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import static com.tournament.tournament_manager.config.kafka.KafkaConfig.MATCH_FINISHED_TOPIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Vérifie que {@link MatchKafkaAdapter} écrit dans l'outbox (voir sa Javadoc) plutôt que
 * d'envoyer directement à Kafka — la publication réelle est testée séparément dans
 * {@code OutboxPublisherServiceTest}.
 */
@ExtendWith(MockitoExtension.class)
class MatchKafkaAdapterTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private final tools.jackson.databind.ObjectMapper objectMapper = JsonMapper.builder().build();

    private MatchKafkaAdapter matchKafkaAdapter;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        matchKafkaAdapter = new MatchKafkaAdapter(outboxEventRepository, objectMapper);
    }

    @Test
    void publishMatchFinished_shouldWriteToOutbox_notKafkaDirectly() {
        MatchFinishedEvent event = new MatchFinishedEvent(1L);

        matchKafkaAdapter.publishMatchFinished(event, 42L);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository, times(1)).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertEquals(MATCH_FINISHED_TOPIC, saved.getTopic());
        assertEquals("42", saved.getPartitionKey());
        assertEquals("MatchFinishedEvent", saved.getEventType());
        assertTrue(saved.getPayload().contains("\"matchId\":1"));
        // Pas encore publié : c'est le rôle d'OutboxPublisherService, pas de cet adapter.
        assertNull(saved.getPublishedAt());
    }
}
