package com.tournament.tournament_manager.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Un refresh token, tel que le domaine métier le connaît.
 *
 * <p>Objet de domaine pur : aucune annotation, aucune dépendance vers JPA, Spring, ou toute
 * autre librairie technique. La persistance est gérée séparément par
 * {@code infrastructure.output.persistence.entity.RefreshTokenEntity} et
 * {@code infrastructure.output.persistence.mapper.RefreshTokenMapper}.
 *
 * <p>{@code token} stocke le hash SHA-256 du refresh token, jamais sa valeur brute
 * (voir {@code RefreshTokenService}).
 */
public class RefreshToken {

    private Long id;
    private String token;
    private String username;
    private Instant expiryDate;
    private boolean revoked;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Instant expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
