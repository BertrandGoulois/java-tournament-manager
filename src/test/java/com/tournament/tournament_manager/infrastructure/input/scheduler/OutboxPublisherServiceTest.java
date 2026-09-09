package com.tournament.tournament_manager.infrastructure.input.scheduler;

import com.tournament.tournament_manager.infrastructure.output.persistence.entity.OutboxEventEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.OutboxEventRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper, new SimpleMeterRegistry());
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
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper, new SimpleMeterRegistry());
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
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper, new SimpleMeterRegistry());
        when(outboxEventRepository.lockNextUnpublishedBatch(anyInt())).thenReturn(List.of());

        outboxPublisherService.publishPendingEvents();

        verify(kafkaTemplate, never()).send(anyString(), any(), any());
        verify(kafkaTemplate, never()).flush();
    }

    /**
     * Point 2.3 de la revue. Les envois doivent tous partir AVANT qu'on attende le moindre
     * accuse de reception. La version precedente faisait send().get(5s) evenement par
     * evenement : sur un lot de 100, une lenteur Kafka gardait la transaction ouverte - et
     * ses verrous FOR UPDATE sur 100 lignes - pendant plus de huit minutes.
     *
     * <p>L'ordre est verifie explicitement parce qu'il n'est pas visible autrement : un
     * refactoring qui remettrait un get() dans la boucle d'envoi laisserait tous les autres
     * tests au vert.
     */
    @Test
    void publishPendingEvents_shouldDispatchAllBeforeAwaitingAny() {
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper, new SimpleMeterRegistry());
        OutboxEventEntity first = pendingEvent(5L, "{\"matchId\":5,\"player1EloBefore\":1000,\"player2EloBefore\":1000}");
        OutboxEventEntity second = pendingEvent(6L, "{\"matchId\":6,\"player1EloBefore\":1000,\"player2EloBefore\":1000}");
        when(outboxEventRepository.lockNextUnpublishedBatch(anyInt()))
                .thenReturn(List.of(first, second));

        SendResult<String, Object> sendResult = new SendResult<>(
                new ProducerRecord<>(MATCH_FINISHED_TOPIC, "42", null), mock(RecordMetadata.class));
        when(kafkaTemplate.send(eq(MATCH_FINISHED_TOPIC), eq("42"), any()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        outboxPublisherService.publishPendingEvents();

        // flush() intervient apres les deux envois et avant toute attente : c'est lui qui
        // vide le buffer producteur, sans quoi linger.ms rendrait l'attente groupee
        // sequentielle malgre tout.
        InOrder inOrder = inOrder(kafkaTemplate);
        inOrder.verify(kafkaTemplate, times(2)).send(eq(MATCH_FINISHED_TOPIC), eq("42"), any());
        inOrder.verify(kafkaTemplate).flush();

        assertNotNull(first.getPublishedAt());
        assertNotNull(second.getPublishedAt());
    }

    @Test
    void publishPendingEvents_shouldContinueWithRemainingEvents_whenOneFails() {
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper, new SimpleMeterRegistry());
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

    /**
     * Point 2.3, second volet. Un payload illisible ne peut pas s'arranger tout seul :
     * l'evenement est abandonne au premier passage plutot que relu toutes les 500 ms pour
     * l'eternite, en inondant les logs au passage.
     */
    @Test
    void publishPendingEvents_shouldAbandonImmediately_whenPayloadIsUnreadable() {
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper, new SimpleMeterRegistry());
        OutboxEventEntity poison = pendingEvent(7L, "ceci n'est pas du JSON");
        when(outboxEventRepository.lockNextUnpublishedBatch(anyInt())).thenReturn(List.of(poison));

        outboxPublisherService.publishPendingEvents();

        assertNull(poison.getPublishedAt());
        assertNotNull(poison.getFailedAt());
        assertNotNull(poison.getLastError());
        // Jamais envoye : l'erreur survient a la deserialisation, avant tout appel a Kafka.
        verify(kafkaTemplate, never()).send(anyString(), any(), any());
    }

    /**
     * Le pendant indispensable du test precedent, et la raison pour laquelle l'abandon ne
     * repose pas sur un compteur de tentatives. Le poller tourne toutes les 500 ms : avec un
     * seuil de tentatives, une panne Kafka de quelques secondes suffirait a jeter TOUS les
     * evenements en attente. Une indisponibilite de Kafka ne doit jamais faire abandonner
     * quoi que ce soit, quelle que soit sa duree.
     */
    @Test
    void publishPendingEvents_shouldNeverAbandon_whenKafkaIsUnavailable() {
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper, new SimpleMeterRegistry());
        OutboxEventEntity event = pendingEvent(8L, "{\"matchId\":8,\"player1EloBefore\":1000,\"player2EloBefore\":1000}");
        when(outboxEventRepository.lockNextUnpublishedBatch(anyInt())).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq(MATCH_FINISHED_TOPIC), eq("42"), any()))
                .thenReturn(CompletableFuture.failedFuture(
                        new org.apache.kafka.common.errors.TimeoutException("broker injoignable")));

        // Cent cycles d'affilee, soit bien au-dela de tout seuil de tentatives plausible.
        for (int i = 0; i < 100; i++) {
            outboxPublisherService.publishPendingEvents();
        }

        assertNull(event.getFailedAt());
        assertNull(event.getPublishedAt());
        // Le compteur avance - c'est de l'observabilite, pas un declencheur d'abandon.
        org.junit.jupiter.api.Assertions.assertEquals(100, event.getAttempts());
    }

    /** Un message trop gros pour le broker ne passera jamais : abandon immediat. */
    @Test
    void publishPendingEvents_shouldAbandon_whenRecordTooLarge() {
        outboxPublisherService = new OutboxPublisherService(outboxEventRepository, kafkaTemplate, objectMapper, new SimpleMeterRegistry());
        OutboxEventEntity event = pendingEvent(9L, "{\"matchId\":9,\"player1EloBefore\":1000,\"player2EloBefore\":1000}");
        when(outboxEventRepository.lockNextUnpublishedBatch(anyInt())).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq(MATCH_FINISHED_TOPIC), eq("42"), any()))
                .thenReturn(CompletableFuture.failedFuture(
                        new org.apache.kafka.common.errors.RecordTooLargeException("2 Mo")));

        outboxPublisherService.publishPendingEvents();

        assertNotNull(event.getFailedAt());
        assertNull(event.getPublishedAt());
    }
}
