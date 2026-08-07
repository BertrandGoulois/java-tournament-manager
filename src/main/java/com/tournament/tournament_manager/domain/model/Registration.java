package com.tournament.tournament_manager.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * L'inscription d'un joueur à un tournoi, telle que le domaine métier la connaît.
 *
 * <p>Objet de domaine pur : aucune annotation, aucune dépendance vers JPA, Spring, ou toute
 * autre librairie technique. La persistance est gérée séparément par
 * {@code infrastructure.output.persistence.entity.RegistrationEntity} et
 * {@code infrastructure.output.persistence.mapper.RegistrationMapper}.
 */
public class Registration {

    private Long id;
    private Instant registeredAt;
    private Tournament tournament;
    private Player player;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Registration that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
