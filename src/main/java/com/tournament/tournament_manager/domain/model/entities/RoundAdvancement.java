package com.tournament.tournament_manager.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Marqueur "ce round a déjà été créé pour ce tournoi", utilisé uniquement comme verrou
 * distribué (voir la contrainte {@code UNIQUE(tournament_id, round)} en base) — jamais lu
 * ni exposé ailleurs que dans {@code AdvanceBracketService}.
 */
@Entity
@Table(name = "round_advancements")
@Getter
@Setter
public class RoundAdvancement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(nullable = false)
    private int round;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
