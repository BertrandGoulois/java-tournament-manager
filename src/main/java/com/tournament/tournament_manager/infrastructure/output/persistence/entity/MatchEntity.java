package com.tournament.tournament_manager.infrastructure.output.persistence.entity;

import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entité JPA pour la persistance d'un match.
 *
 * <p>Contrepartie technique du domaine pur {@code domain.model.Match} — voir
 * {@code MatchMapper} pour la conversion entre les deux.
 */
@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
public class MatchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int round;

    @Column(nullable = false)
    private int position;

    @Column(name = "group_number")
    private Integer groupNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.PENDING;

    @Column
    private Instant playedAt;

    @Column(columnDefinition = "TEXT")
    private String commentary;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne()
    @JoinColumn(name = "tournament_id", nullable = false)
    private TournamentEntity tournament;

    @ManyToOne()
    @JoinColumn(name = "player1_id", nullable = false)
    private PlayerEntity player1;

    @ManyToOne()
    @JoinColumn(name = "player2_id", nullable = true)
    private PlayerEntity player2;

    @ManyToOne()
    @JoinColumn(name = "winner_id", nullable = true)
    private PlayerEntity winner;
}
