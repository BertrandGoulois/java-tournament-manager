package com.tournament.tournament_manager.domain.model;

import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.exception.domain.InvalidException;

import java.time.Instant;
import java.util.Objects;

/**
 * Un match, tel que le domaine métier le connaît.
 *
 * <p>Objet de domaine pur : aucune annotation, aucune dépendance vers JPA, Spring, ou toute
 * autre librairie technique. La persistance est gérée séparément par
 * {@code infrastructure.output.persistence.entity.MatchEntity} et
 * {@code infrastructure.output.persistence.mapper.MatchMapper}.
 *
 * <p>Pas de setters publics : enregistrer un résultat passe exclusivement par
 * {@link #recordResult(Long)}, qui valide que le match n'est pas déjà terminé et que le
 * vainqueur désigné est bien l'un des deux participants - ces deux règles vivaient
 * auparavant dans {@code RecordMatchResultService}, dispersées loin de la donnée qu'elles
 * protègent.
 */
public class Match {

    private Long id;
    private int round;
    private int position;
    private Integer groupNumber;
    private MatchStatus status = MatchStatus.PENDING;
    private Instant playedAt;
    private String commentary;
    private Tournament tournament;
    private Player player1;
    private Player player2;
    private Player winner;

    private Match() {
    }

    /**
     * Planifie un nouveau match. Si {@code player2} est {@code null} (bye), le match est
     * immédiatement marqué terminé avec {@code player1} pour vainqueur.
     */
    public static Match schedule(Tournament tournament, int round, int position, Integer groupNumber,
                                 Player player1, Player player2) {
        Match match = new Match();
        match.tournament = tournament;
        match.round = round;
        match.position = position;
        match.groupNumber = groupNumber;
        match.player1 = player1;
        match.player2 = player2;
        if (player2 == null) {
            match.winner = player1;
            match.status = MatchStatus.FINISHED;
            match.playedAt = Instant.now();
        }
        return match;
    }

    /**
     * Reconstruit un match depuis un état déjà persisté. Réservé à
     * {@code MatchMapper} - ne jamais appeler depuis un service applicatif.
     */
    public static Match reconstitute(Long id, int round, int position, Integer groupNumber,
                                     MatchStatus status, Instant playedAt, String commentary,
                                     Tournament tournament, Player player1, Player player2, Player winner) {
        Match match = new Match();
        match.id = id;
        match.round = round;
        match.position = position;
        match.groupNumber = groupNumber;
        match.status = status;
        match.playedAt = playedAt;
        match.commentary = commentary;
        match.tournament = tournament;
        match.player1 = player1;
        match.player2 = player2;
        match.winner = winner;
        return match;
    }

    /**
     * Enregistre le résultat du match.
     *
     * @param winnerId identifiant du joueur vainqueur - doit être {@code player1} ou {@code player2}
     * @throws InvalidException si le match est déjà terminé
     * @throws InvalidException si {@code winnerId} ne correspond à aucun des deux participants
     */
    public void recordResult(Long winnerId) {
        if (status == MatchStatus.FINISHED) {
            throw new InvalidException("Match already finished");
        }
        this.winner = resolveParticipant(winnerId);
        this.status = MatchStatus.FINISHED;
        this.playedAt = Instant.now();
    }

    private Player resolveParticipant(Long playerId) {
        if (player1.getId().equals(playerId)) {
            return player1;
        }
        if (player2 != null && player2.getId().equals(playerId)) {
            return player2;
        }
        throw new InvalidException("Winner is not a player of this match");
    }

    public Long getId() {
        return id;
    }

    public int getRound() {
        return round;
    }

    public int getPosition() {
        return position;
    }

    public Integer getGroupNumber() {
        return groupNumber;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public String getCommentary() {
        return commentary;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public Player getWinner() {
        return winner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Match match)) return false;
        return id != null && id.equals(match.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
