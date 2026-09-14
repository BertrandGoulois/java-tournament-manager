package com.tournament.tournament_manager.infrastructure.output.persistence.entity;

import java.time.Instant;

import jakarta.persistence.*;

import com.tournament.tournament_manager.domain.model.enums.RoundAdvancementStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    /**
     * Voir {@link RoundAdvancementStatus} et la migration 019. Pas de valeur par defaut ici :
     * c'est {@code RoundAdvancementMapper.toNewEntity} qui pose PENDING, pour qu'un claim
     * naisse toujours non confirme.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoundAdvancementStatus status;
}
