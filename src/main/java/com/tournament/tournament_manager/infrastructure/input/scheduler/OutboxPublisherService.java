package com.tournament.tournament_manager.infrastructure.input.scheduler;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.OutboxEventEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Publie vers Kafka les événements écrits dans l'outbox transactionnel (voir
 * {@code MatchKafkaAdapter} et la migration {@code 012-add-outbox-events.sql}).
 *
 * <p>Tourne toutes les 500ms. Chaque cycle verrouille un lot d'événements non publiés
 * ({@code FOR UPDATE SKIP LOCKED} — sûr avec plusieurs instances de l'application en
 * parallèle), tente de les envoyer à Kafka, et marque {@code publishedAt} pour ceux
 * envoyés avec succès. Un échec d'envoi (Kafka indisponible, timeout...) laisse
 * l'événement non publié : il sera retenté au cycle suivant, sans jamais être perdu — la
 * ligne reste en base tant qu'elle n'a pas été confirmée publiée.
 */
@Slf4j
@Component
public class OutboxPublisherService {

    private static final int BATCH_SIZE = 100;
    private static final long SEND_TIMEOUT_SECONDS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository,
                                  KafkaTemplate<String, Object> kafkaTemplate,
                                  ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEventEntity> batch = outboxEventRepository.lockNextUnpublishedBatch(BATCH_SIZE);
        if (batch.isEmpty()) {
            return;
        }
        log.debug("Publication de {} événement(s) en attente depuis l'outbox", batch.size());
        for (OutboxEventEntity event : batch) {
            publishOne(event);
        }
    }

    private void publishOne(OutboxEventEntity event) {
        try {
            Object payload = deserializePayload(event);
            kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), payload)
                    .get(SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            event.setPublishedAt(Instant.now());
        } catch (Exception e) {
            // Événement laissé non publié : retenté au prochain cycle (500ms). Le verrou
            // FOR UPDATE SKIP LOCKED garantit qu'aucune autre instance ne le publie en
            // double entre-temps.
            log.error("Échec de publication d'un événement outbox, nouvelle tentative au "
                    + "prochain cycle [id={}, topic={}]", event.getId(), event.getTopic(), e);
        }
    }

    private Object deserializePayload(OutboxEventEntity event) {
        // Un seul type d'événement existe aujourd'hui dans tout le système. À généraliser
        // (switch sur event.getEventType()) si un second type d'événement outbox apparaît.
        return objectMapper.readValue(event.getPayload(), MatchFinishedEvent.class);
    }
}
