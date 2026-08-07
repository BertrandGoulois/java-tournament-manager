package com.tournament.tournament_manager.infrastructure.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entité JPA pour la persistance du marqueur d'avancement de round. Contrepartie technique
 * du domaine pur {@code domain.model.RoundAdvancement} — voir {@code RoundAdvancementMapper}.
 */
@Entity
@Table(name = "round_advancements")
@Getter
@Setter
@NoArgsConstructor
public class RoundAdvancementEntity {

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
