package com.tournament.tournament_manager.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Une entrée d'historique ELO, telle que le domaine métier la connaît.
 *
 * <p>Objet de domaine pur : aucune annotation, aucune dépendance vers JPA, Spring, ou toute
 * autre librairie technique. La persistance est gérée séparément par
 * {@code infrastructure.output.persistence.entity.EloHistoryEntity} et
 * {@code infrastructure.output.persistence.mapper.EloHistoryMapper}.
 */
public class EloHistory {

    private Long id;
    private int eloChange;
    private int eloAfter;
    private Instant createdAt;
    private Player player;
    private Match match;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getEloChange() {
        return eloChange;
    }

    public void setEloChange(int eloChange) {
        this.eloChange = eloChange;
    }

    public int getEloAfter() {
        return eloAfter;
    }

    public void setEloAfter(int eloAfter) {
        this.eloAfter = eloAfter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EloHistory that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
