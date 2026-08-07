package com.tournament.tournament_manager.domain.model;

import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Un tournoi, tel que le domaine métier le connaît.
 *
 * <p>Objet de domaine pur : aucune annotation, aucune dépendance vers JPA, Spring, ou toute
 * autre librairie technique. La persistance est gérée séparément par
 * {@code infrastructure.output.persistence.entity.TournamentEntity} et
 * {@code infrastructure.output.persistence.mapper.TournamentMapper}.
 */
public class Tournament {

    private Long id;
    private String name;
    private TournamentStatus status = TournamentStatus.OPEN;
    private TournamentFormat format = TournamentFormat.SINGLE_ELIMINATION;
    private Integer numberOfGroups;
    private Integer qualifiersPerGroup;
    private int maxPlayers;
    private Instant createdAt;
    private boolean deleted;
    private Instant deletedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public void setStatus(TournamentStatus status) {
        this.status = status;
    }

    public TournamentFormat getFormat() {
        return format;
    }

    public void setFormat(TournamentFormat format) {
        this.format = format;
    }

    public Integer getNumberOfGroups() {
        return numberOfGroups;
    }

    public void setNumberOfGroups(Integer numberOfGroups) {
        this.numberOfGroups = numberOfGroups;
    }

    public Integer getQualifiersPerGroup() {
        return qualifiersPerGroup;
    }

    public void setQualifiersPerGroup(Integer qualifiersPerGroup) {
        this.qualifiersPerGroup = qualifiersPerGroup;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tournament tournament)) return false;
        return id != null && id.equals(tournament.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
