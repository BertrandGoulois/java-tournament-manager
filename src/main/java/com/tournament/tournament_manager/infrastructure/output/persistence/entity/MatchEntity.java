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
 *
 * <p><b>§4 de la revue — associations en LAZY.</b> Les quatre {@code @ManyToOne} étaient
 * implicitement EAGER (le défaut JPA, contre-intuitif) : charger un match tirait
 * systématiquement le tournoi et les trois joueurs, même quand l'appelant ne voulait que
 * le score.
 *
 * <p>Attention, le passage en LAZY <b>n'est pas gratuit ici</b> et ne se suffit pas à
 * lui-même : {@code MatchMapper.toDomain} déréférence les quatre associations sans
 * condition, à chaque conversion. Une association LAZY non préchargée par la requête ne
 * disparaît donc pas — elle se transforme en un SELECT supplémentaire au moment du mapping,
 * ce qui est <i>pire</i> que la jointure EAGER qu'elle remplace. Le gain n'existe que si la
 * requête qui charge l'entité fait le {@code JOIN FETCH} correspondant : voir
 * {@code MatchRepository}, dont toutes les méthodes de lecture le font désormais, y compris
 * {@code findByIdWithAssociations} ajoutée pour le chemin {@code loadMatch}.
 *
 * <p>Corollaire à garder en tête : toute nouvelle requête renvoyant des {@code MatchEntity}
 * destinés au mapper doit préciser ses {@code JOIN FETCH}. {@code open-in-view=false} rend
 * l'oubli visible (LazyInitializationException hors transaction) plutôt que silencieux,
 * mais à l'intérieur d'une transaction il ne se manifeste que par un N+1.
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private TournamentEntity tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id", nullable = false)
    private PlayerEntity player1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id", nullable = true)
    private PlayerEntity player2;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id", nullable = true)
    private PlayerEntity winner;
}
