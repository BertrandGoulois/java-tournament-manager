package com.tournament.tournament_manager.domain.model;

import com.tournament.tournament_manager.domain.model.enums.MatchStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Un match, tel que le domaine métier le connaît.
 *
 * <p>Objet de domaine pur : aucune annotation, aucune dépendance vers JPA, Spring, ou toute
 * autre librairie technique. La persistance est gérée séparément par
 * {@code infrastructure.output.persistence.entity.MatchEntity} et
 * {@code infrastructure.output.persistence.mapper.MatchMapper}.
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public Integer getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(Integer groupNumber) {
        this.groupNumber = groupNumber;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public Instant getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(Instant playedAt) {
        this.playedAt = playedAt;
    }

    public String getCommentary() {
        return commentary;
    }

    public void setCommentary(String commentary) {
        this.commentary = commentary;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Player getPlayer1() {
        return player1;
    }

    public void setPlayer1(Player player1) {
        this.player1 = player1;
    }

    public Player getPlayer2() {
        return player2;
    }

    public void setPlayer2(Player player2) {
        this.player2 = player2;
    }

    public Player getWinner() {
        return winner;
    }

    public void setWinner(Player winner) {
        this.winner = winner;
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
