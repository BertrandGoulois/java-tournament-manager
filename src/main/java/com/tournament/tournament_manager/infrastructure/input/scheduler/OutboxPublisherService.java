package com.tournament.tournament_manager.infrastructure.input.scheduler;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.OutboxEventEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publie vers Kafka les événements écrits dans l'outbox transactionnel (voir
 * {@code MatchKafkaAdapter} et la migration {@code 012-add-outbox-events.sql}).
 *
 * <p>Tourne toutes les 500ms. Chaque cycle verrouille un lot d'événements non publiés
 * ({@code FOR UPDATE SKIP LOCKED} — sûr avec plusieurs instances de l'application en
 * parallèle), les envoie à Kafka, et marque {@code publishedAt} pour ceux confirmés. Un
 * échec d'envoi (Kafka indisponible, timeout...) laisse l'événement non publié : il sera
 * retenté au cycle suivant, sans jamais être perdu — la ligne reste en base tant qu'elle
 * n'a pas été confirmée publiée.
 *
 * <p><b>Point 2.3 de la revue — envois parallèles, attente groupée.</b> La version
 * précédente attendait l'accusé de réception de chaque événement avant d'envoyer le suivant
 * ({@code send(...).get(5s)} dans une boucle). Les envois étaient donc sérialisés, et le
 * pire cas se calculait en multipliant : 100 événements × 5 s de timeout = plus de huit
 * minutes pendant lesquelles la transaction reste ouverte et garde ses verrous
 * {@code FOR UPDATE} sur 100 lignes. Une lenteur de Kafka se transformait ainsi en blocage
 * prolongé côté PostgreSQL, avec un ordonnanceur qui relance un cycle toutes les 500 ms
 * par-dessus.
 *
 * <p>Les envois partent maintenant tous d'abord (le producteur Kafka les regroupe lui-même),
 * puis les accusés sont attendus sous une <b>échéance globale</b> et non par événement. Le
 * pire cas est borné par {@link #BATCH_TIMEOUT} quel que soit le nombre d'événements.
 *
 * <p><b>Second volet du point 2.3 : les événements empoisonnés.</b> Un événement dont le
 * payload est illisible était retenté indéfiniment, toutes les 500 ms, avec un
 * {@code log.error} à chaque passage — inondant les logs sans jamais progresser, et occupant
 * une place dans chaque lot au détriment des événements sains. Ces événements sont
 * désormais abandonnés explicitement ({@code failed_at}, voir migration 020) et cessent
 * d'être relus.
 *
 * <p><b>L'abandon ne repose délibérément pas sur un compteur de tentatives</b>, contrairement
 * au réflexe habituel. Le poller tourne toutes les 500 ms : une panne Kafka de trois secondes
 * épuiserait n'importe quel seuil raisonnable et jetterait <i>tous</i> les événements en
 * attente, transformant un incident passager en perte de données définitive. Le critère est
 * la <b>nature</b> de l'erreur, pas sa répétition — voir {@link #isPermanentFailure}. Une
 * panne d'infrastructure laisse l'événement en attente aussi longtemps qu'il le faut, ce qui
 * est le seul comportement correct pour un outbox. Le compteur {@code attempts} existe, mais
 * pour le diagnostic uniquement.
 */
@Slf4j
@Component
public class OutboxPublisherService {

    private static final int BATCH_SIZE = 100;

    /**
     * Échéance pour l'ensemble du lot, et non par événement. Un lot qui déborde laisse
     * simplement ses événements non publiés pour le cycle suivant : rien n'est perdu, et la
     * transaction est relâchée à temps.
     */
    private static final Duration BATCH_TIMEOUT = Duration.ofSeconds(10);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * §8 de la revue : profondeur de l'outbox exposée en métrique. Sans elle, un outbox qui
     * se remplit — Kafka en panne, publisher bloqué — est invisible jusqu'à ce que la
     * conséquence métier se voie, c'est-à-dire des tournois qui n'avancent plus.
     *
     * <p>Alimentée à chaque cycle plutôt que calculée au moment du scrape : la métrique ne
     * doit pas déclencher un COUNT en base à chaque passage de Prometheus.
     */
    private final AtomicLong pendingEventsGauge = new AtomicLong(0);

    /**
     * Comme {@link #pendingEventsGauge}, alimentée depuis le cycle du poller et non calculée
     * au moment du scrape : une jauge ne doit pas déclencher de requête en base à chaque
     * passage de Prometheus, ni s'exécuter hors transaction sur le thread des métriques.
     */
    private final AtomicLong failedEventsGauge = new AtomicLong(0);

    private final Counter abandonedEventsCounter;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository,
                                  KafkaTemplate<String, Object> kafkaTemplate,
                                  ObjectMapper objectMapper,
                                  MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        Gauge.builder("outbox.pending.events", pendingEventsGauge, AtomicLong::get)
                .description("Événements outbox en attente de publication vers Kafka")
                .register(meterRegistry);
        Gauge.builder("outbox.failed.events", failedEventsGauge, AtomicLong::get)
                .description("Événements outbox définitivement abandonnés — toute valeur non "
                        + "nulle demande une intervention humaine")
                .register(meterRegistry);
        this.abandonedEventsCounter = Counter.builder("outbox.events.abandoned")
                .description("Événements outbox abandonnés pour erreur non rejouable")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingEvents() {
        failedEventsGauge.set(outboxEventRepository.countByFailedAtIsNotNull());

        List<OutboxEventEntity> batch = outboxEventRepository.lockNextUnpublishedBatch(BATCH_SIZE);
        if (batch.isEmpty()) {
            pendingEventsGauge.set(0);
            return;
        }
        log.debug("Publication de {} événement(s) en attente depuis l'outbox", batch.size());

        List<PendingSend> sent = dispatchAll(batch);
        int confirmed = awaitConfirmations(sent);

        // Un lot plein signifie qu'il reste probablement des événements au-delà : la valeur
        // est un plancher, pas un compte exact. C'est suffisant pour alerter sur une dérive,
        // et ça évite un COUNT(*) complet à chaque cycle.
        pendingEventsGauge.set((long) batch.size() - confirmed);
    }

    /**
     * Envoie tout le lot sans attendre, puis force la vidange du buffer producteur.
     *
     * <p>{@code flush()} est ce qui rend l'attente groupée réellement parallèle : sans lui,
     * le producteur peut retenir les messages jusqu'à {@code linger.ms}, et le premier
     * {@code get()} déclencherait de fait un envoi séquentiel.
     */
    private List<PendingSend> dispatchAll(List<OutboxEventEntity> batch) {
        List<PendingSend> pending = new ArrayList<>(batch.size());
        for (OutboxEventEntity event : batch) {
            try {
                Object payload = deserializePayload(event);
                CompletableFuture<SendResult<String, Object>> future =
                        kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), payload);
                pending.add(new PendingSend(event, future));
            } catch (Exception e) {
                // Échec avant même l'envoi : payload illisible, ou producteur qui refuse.
                recordFailure(event, e);
            }
        }
        if (!pending.isEmpty()) {
            kafkaTemplate.flush();
        }
        return pending;
    }

    /**
     * Attend les accusés sous une échéance commune. Chaque {@code get()} reçoit le temps
     * <i>restant</i> et non un timeout complet : c'est ce qui empêche les attentes de
     * s'additionner.
     */
    private int awaitConfirmations(List<PendingSend> pending) {
        Instant deadline = Instant.now().plus(BATCH_TIMEOUT);
        Instant now = Instant.now();
        int confirmed = 0;

        for (PendingSend send : pending) {
            long remainingMillis = Duration.between(Instant.now(), deadline).toMillis();
            if (remainingMillis <= 0) {
                log.error("Échéance du lot outbox dépassée, événements restants laissés non "
                        + "publiés pour le prochain cycle [id={}]", send.event().getId());
                break;
            }
            try {
                send.future().get(remainingMillis, TimeUnit.MILLISECONDS);
                send.event().setPublishedAt(now);
                confirmed++;
            } catch (TimeoutException e) {
                // Toujours transitoire : on ne pénalise pas un événement pour une lenteur.
                send.event().setAttempts(send.event().getAttempts() + 1);
                log.error("Délai dépassé sur la confirmation d'un événement outbox, nouvelle "
                        + "tentative au prochain cycle [id={}, topic={}]",
                        send.event().getId(), send.event().getTopic());
                break;
            } catch (Exception e) {
                // Échec propre à cet événement : les suivants peuvent encore aboutir.
                // get() enveloppe la vraie cause dans une ExecutionException.
                Throwable cause = (e instanceof ExecutionException && e.getCause() != null)
                        ? e.getCause() : e;
                recordFailure(send.event(), cause);
            }
        }
        return confirmed;
    }

    /**
     * Comptabilise un échec et décide du sort de l'événement : abandon définitif si l'erreur
     * ne peut pas se résoudre d'elle-même, simple report au cycle suivant sinon.
     */
    private void recordFailure(OutboxEventEntity event, Throwable cause) {
        event.setAttempts(event.getAttempts() + 1);

        if (!isPermanentFailure(cause)) {
            log.error("Échec de publication d'un événement outbox, nouvelle tentative au "
                            + "prochain cycle [id={}, topic={}, tentatives={}]",
                    event.getId(), event.getTopic(), event.getAttempts(), cause);
            return;
        }

        event.setFailedAt(Instant.now());
        event.setLastError(cause.getClass().getName() + " : " + cause.getMessage());
        abandonedEventsCounter.increment();
        // Un seul log d'abandon par événement, puisqu'il ne sera plus jamais relu — à
        // l'opposé du log.error répété toutes les 500 ms de la version précédente.
        log.error("Événement outbox ABANDONNÉ définitivement, il ne sera plus retenté et "
                        + "demande une intervention manuelle [id={}, topic={}, type={}] : {}",
                event.getId(), event.getTopic(), event.getEventType(), cause.toString());
    }

    /**
     * {@code true} si réessayer ne peut rien changer.
     *
     * <p>C'est le cœur de la décision d'abandon, et la raison pour laquelle un compteur de
     * tentatives serait le mauvais critère : à 500 ms par cycle, n'importe quel seuil serait
     * franchi par une panne Kafka de quelques secondes, et l'outbox jetterait des événements
     * parfaitement valides. Ici, une indisponibilité de Kafka n'entraîne jamais d'abandon,
     * quelle que soit sa durée.
     *
     * <p>Sont considérées comme définitives les erreurs qui tiennent au <b>contenu</b> du
     * message : payload illisible, message dépassant la taille maximale du broker, nom de
     * topic invalide. Aucune de ces situations ne s'améliore en attendant.
     *
     * <p>En cas de doute, l'erreur est traitée comme transitoire : retenir trop longtemps un
     * événement irrécupérable coûte du bruit, l'abandonner à tort coûte une perte de données.
     */
    private boolean isPermanentFailure(Throwable cause) {
        return cause instanceof JacksonException
                || cause instanceof SerializationException
                || cause instanceof RecordTooLargeException
                || cause instanceof InvalidTopicException;
    }

    private Object deserializePayload(OutboxEventEntity event) {
        // Un seul type d'événement existe aujourd'hui dans tout le système. À généraliser
        // (switch sur event.getEventType()) si un second type d'événement outbox apparaît.
        return objectMapper.readValue(event.getPayload(), MatchFinishedEvent.class);
    }

    /** Association d'un événement et de l'accusé de réception qu'on attend pour lui. */
    private record PendingSend(OutboxEventEntity event,
                               CompletableFuture<SendResult<String, Object>> future) {}
}
