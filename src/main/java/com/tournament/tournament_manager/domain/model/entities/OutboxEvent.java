package com.tournament.tournament_manager.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Ligne de l'outbox transactionnel : un événement écrit dans la même transaction que le
 * changement métier qui l'a déclenché, en attente d'être réellement publié sur Kafka par
 * {@code OutboxPublisherService}.
 *
 * <p>{@code publishedAt == null} signifie "pas encore publié" — c'est ce que le poller
 * interroge à chaque cycle.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Topic Kafka de destination (ex. {@code match-finished}). */
    @Column(nullable = false)
    private String topic;

    /**
     * Clé de partition Kafka (ex. l'identifiant du tournoi), pour garantir l'ordre des
     * événements d'un même agrégat une fois le topic partitionné en plusieurs partitions.
     */
    @Column(name = "partition_key", nullable = false)
    private String partitionKey;

    /**
     * Nom du type d'événement (ex. {@code MatchFinishedEvent}), utilisé par le poller pour
     * savoir vers quelle classe Java désérialiser {@link #payload} avant publication.
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /** Représentation JSON de l'événement. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant publishedAt;
}
