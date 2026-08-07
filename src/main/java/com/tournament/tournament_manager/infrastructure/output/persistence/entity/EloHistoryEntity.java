package com.tournament.tournament_manager.infrastructure.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entité JPA pour la persistance d'une entrée d'historique ELO.
 *
 * <p>Contrepartie technique du domaine pur {@code domain.model.EloHistory} — voir
 * {@code EloHistoryMapper} pour la conversion entre les deux.
 */
@Entity
@Table(name = "elo_history")
@Getter
@Setter
@NoArgsConstructor
public class EloHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int eloChange;

    @Column(nullable = false)
    private int eloAfter;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToOne()
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @ManyToOne()
    @JoinColumn(name = "match_id", nullable = false)
    private MatchEntity match;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
