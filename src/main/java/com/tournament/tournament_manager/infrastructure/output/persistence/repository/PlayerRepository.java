package com.tournament.tournament_manager.infrastructure.output.persistence.repository;

import com.tournament.tournament_manager.domain.model.entities.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    Optional<Player> findByUsername(String username);
    Optional<Player> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /**
     * Anonymise les joueurs soft-deleted depuis plus de {@code retentionLimit} qui ont un
     * historique (au moins un match ou une inscription) : leurs données personnelles
     * (username, email) sont écrasées par une valeur générique dérivée de leur id, mais la
     * ligne est conservée — elle reste référencée par {@code matches}, {@code registrations}
     * et {@code elo_history}, et ces tables n'ont pas de {@code ON DELETE CASCADE} vers
     * {@code players} (par choix : l'historique sportif d'un tournoi ne doit pas disparaître
     * parce qu'un participant a demandé la suppression de son compte).
     *
     * <p>{@code anonymized_at IS NULL} dans la clause garantit qu'un joueur déjà anonymisé
     * n'est plus jamais retraité par les exécutions suivantes du job.
     */
    @Modifying
    @Query(value = """
            UPDATE players
            SET username = 'utilisateur-supprime-' || id,
                email = 'supprime-' || id || '@anonymise.invalid',
                anonymized_at = NOW()
            WHERE deleted = true
              AND deleted_at < :retentionLimit
              AND anonymized_at IS NULL
              AND (
                EXISTS (SELECT 1 FROM matches m
                        WHERE m.player1_id = players.id OR m.player2_id = players.id OR m.winner_id = players.id)
                OR EXISTS (SELECT 1 FROM registrations r WHERE r.player_id = players.id)
              )
            """, nativeQuery = true)
    int anonymizeWithHistory(LocalDateTime retentionLimit);

    /**
     * Supprime physiquement les joueurs soft-deleted depuis plus de {@code retentionLimit}
     * qui n'ont <b>aucun</b> historique (ni match, ni inscription) — les seuls que l'on peut
     * supprimer sans risquer une violation de contrainte FK ni amputer l'historique d'un
     * autre joueur.
     *
     * <p>À appeler après {@link #anonymizeWithHistory}, pas avant : sinon les joueurs avec
     * historique seraient encore candidats à ce DELETE et feraient à nouveau échouer la
     * requête sur la contrainte FK.
     *
     * <p>Utilise une requête native pour bypasser le {@code @SQLRestriction("deleted = false")}
     * qui empêche JPA de voir les entités soft-deleted.
     */
    @Modifying
    @Query(value = """
            DELETE FROM players
            WHERE deleted = true
              AND deleted_at < :retentionLimit
              AND NOT EXISTS (SELECT 1 FROM matches m
                              WHERE m.player1_id = players.id OR m.player2_id = players.id OR m.winner_id = players.id)
              AND NOT EXISTS (SELECT 1 FROM registrations r WHERE r.player_id = players.id)
            """, nativeQuery = true)
    int purgeDeletedWithoutHistory(LocalDateTime retentionLimit);
}