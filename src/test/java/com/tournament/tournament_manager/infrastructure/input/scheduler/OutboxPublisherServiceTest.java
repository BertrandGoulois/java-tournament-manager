package com.tournament.tournament_manager.infrastructure.input.scheduler;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.OutboxEventEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.tournament.tournament_manager.config.kafka.KafkaConfig.MATCH_FINISHED_TOPIC;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OutboxPublisherService outboxPublisherService;

    private final tools.jackson.databind.ObjectMapper objectMapper = JsonMapper.builder().build();

    private OutboxEventEntity pendingEvent(long id, String payload) {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(id);
        event.setTopic(MATCH_FINISHED_TOPIC);
        event.setPartitionKey("42");
        event.setEventType("MatchFinishedEvent");
        event.setPayload(payload);
        return event;
    }

    @Test
    void publishPendingEvents_shouldMarkPublished_whenSendSucceeds() {
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper);
        OutboxEventEntity event = pendingEvent(1L, "{\"matchId\":1,\"player1EloBefore\":1000,\"player2EloBefore\":1000}");
        when(outboxEventRepository.lockNextUnpublishedBatch(anyInt())).thenReturn(List.of(event));

        SendResult<String, Object> sendResult = new SendResult<>(
                new ProducerRecord<>(MATCH_FINISHED_TOPIC, "42", null), mock(RecordMetadata.class));
        when(kafkaTemplate.send(eq(MATCH_FINISHED_TOPIC), eq("42"), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        outboxPublisherService.publishPendingEvents();

        assertNotNull(event.getPublishedAt());
    }

    @Test
    void publishPendingEvents_shouldLeaveUnpublished_whenSendFails() {
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper);
        OutboxEventEntity event = pendingEvent(2L, "{\"matchId\":2,\"player1EloBefore\":1000,\"player2EloBefore\":1000}");
        when(outboxEventRepository.lockNextUnpublishedBatch(anyInt())).thenReturn(List.of(event));

        when(kafkaTemplate.send(eq(MATCH_FINISHED_TOPIC), eq("42"), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka indisponible")));

        outboxPublisherService.publishPendingEvents();

        // L'événement reste non publié : il sera retenté au cycle suivant, pas perdu.
        assertNull(event.getPublishedAt());
    }

    @Test
    void publishPendingEvents_shouldDoNothing_whenNoBatch() {
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper);
        when(outboxEventRepository.lockNextUnpublishedBatch(anyInt())).thenReturn(List.of());

        outboxPublisherService.publishPendingEvents();

        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }

    @Test
    void publishPendingEvents_shouldContinueWithRemainingEvents_whenOneFails() {
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper);
        OutboxEventEntity failing = pendingEvent(3L, "{\"matchId\":3,\"player1EloBefore\":1000,\"player2EloBefore\":1000}");
        OutboxEventEntity succeeding = pendingEvent(4L, "{\"matchId\":4,\"player1EloBefore\":1000,\"player2EloBefore\":1000}");
        when(outboxEventRepository.lockNextUnpublishedBatch(anyInt()))
                .thenReturn(List.of(failing, succeeding));

        when(kafkaTemplate.send(eq(MATCH_FINISHED_TOPIC), eq("42"), argThat(m ->
                m != null && m.toString().contains("matchId=3"))))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("boom")));

        SendResult<String, Object> sendResult = new SendResult<>(
                new ProducerRecord<>(MATCH_FINISHED_TOPIC, "42", null), mock(RecordMetadata.class));
        when(kafkaTemplate.send(eq(MATCH_FINISHED_TOPIC), eq("42"), argThat(m ->
                m != null && m.toString().contains("matchId=4"))))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        outboxPublisherService.publishPendingEvents();

        assertNull(failing.getPublishedAt());
        assertNotNull(succeeding.getPublishedAt());
    }
}
