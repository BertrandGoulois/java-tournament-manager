package com.tournament.tournament_manager.infrastructure.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entité JPA pour la persistance d'une ligne outbox. Contrepartie technique du domaine pur
 * {@code domain.model.OutboxEvent} — voir {@code OutboxEventMapper}.
 */
@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "partition_key", nullable = false)
    private String partitionKey;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant publishedAt;

    /**
     * Nombre de tentatives de publication echouees. Purement informatif : l'abandon ne
     * repose PAS sur ce compteur (voir la migration 020 et
     * {@code OutboxPublisherService.isPermanentFailure}). Il sert au diagnostic et permet de
     * reperer un evenement qui rame sans etre pour autant irrecuperable.
     */
    @Column(nullable = false)
    private int attempts = 0;

    /**
     * Horodatage de l'abandon definitif. {@code null} tant que l'evenement reste candidat a
     * la publication ; renseigne uniquement sur erreur non rejouable.
     */
    private Instant failedAt;

    /** Cause de l'abandon, pour ne pas avoir a retrouver la ligne de log correspondante. */
    @Column(columnDefinition = "TEXT")
    private String lastError;
}
