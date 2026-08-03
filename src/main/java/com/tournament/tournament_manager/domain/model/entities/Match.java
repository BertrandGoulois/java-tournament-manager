package com.tournament.tournament_manager.domain.model.entities;

import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Getter
@Setter
@NoArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int round;

    @Column(name = "group_number")
    private Integer groupNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.PENDING;

    /**
     * Date à laquelle le résultat du match a été enregistré. Renseigné par
     * {@code RecordMatchResultService} pour un match réellement joué, et par
     * {@code BracketUtils.createMatch} à l'insertion pour un bye.
     */
    @Column
    private LocalDateTime playedAt;

    @Column(columnDefinition = "TEXT")
    private String commentary;

    @Version
    @Column(nullable = false)
    private Long version;

    @ManyToOne()
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne()
    @JoinColumn(name = "player1_id", nullable = false)
    private Player player1;

    @ManyToOne()
    @JoinColumn(name = "player2_id", nullable = true)
    private Player player2;

    @ManyToOne()
    @JoinColumn(name = "winner_id", nullable = true)
    private Player winner;

}
