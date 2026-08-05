package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.domain.model.entities.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUsername(String username);

    /**
     * Supprime physiquement les refresh tokens expirés depuis plus de {@code cutoff}.
     * Les tokens révoqués mais pas encore expirés sont conservés (utile pour la
     * détection de réutilisation) ; seuls les tokens dont la fenêtre d'utilisation
     * est de toute façon terminée sont purgés.
     *
     * @param cutoff date limite : les tokens expirés avant cette date sont purgés
     * @return le nombre de lignes supprimées
     */
    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.expiryDate < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}