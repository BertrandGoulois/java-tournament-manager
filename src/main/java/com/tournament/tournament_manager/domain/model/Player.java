package com.tournament.tournament_manager.domain.model;

import com.tournament.tournament_manager.domain.model.valueobjects.EloRating;

import java.time.Instant;
import java.util.Objects;

/**
 * Un joueur, tel que le domaine métier le connaît.
 *
 * <p>Objet de domaine pur : aucune annotation, aucune dépendance vers JPA, Spring, ou toute
 * autre librairie technique. La persistance est gérée séparément par
 * {@code infrastructure.output.persistence.entity.PlayerEntity} et
 * {@code infrastructure.output.persistence.mapper.PlayerMapper} (voir leur Javadoc).
 *
 * <p>{@code version} n'est pas exposé ici : c'est un détail de verrouillage optimiste propre
 * à la persistance JPA (voir {@code PlayerEntity.version}), sans signification métier — le
 * mapper le porte en aller-retour via {@code PlayerMapper} pour que
 * {@code save(mapper.toEntity(player, previousVersion))} fonctionne, mais le domaine n'a
 * jamais besoin de le lire ni de le manipuler.
 */
public class Player {

    private Long id;
    private String username;
    private String email;
    private EloRating eloRating;
    private Instant createdAt;
    private boolean deleted;
    private Instant deletedAt;
    private Instant anonymizedAt;

    public Player() {
        this.eloRating = EloRating.defaultRating();
    }

    public Player(Long id, String username, String email, EloRating eloRating,
                  Instant createdAt, boolean deleted, Instant deletedAt, Instant anonymizedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.eloRating = eloRating;
        this.createdAt = createdAt;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.anonymizedAt = anonymizedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public EloRating getEloRating() {
        return eloRating;
    }

    public void setEloRating(EloRating eloRating) {
        this.eloRating = eloRating;
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

    public Instant getAnonymizedAt() {
        return anonymizedAt;
    }

    public void setAnonymizedAt(Instant anonymizedAt) {
        this.anonymizedAt = anonymizedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Player player)) return false;
        return id != null && id.equals(player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
