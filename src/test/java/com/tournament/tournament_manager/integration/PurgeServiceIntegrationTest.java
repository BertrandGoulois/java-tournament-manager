package com.tournament.tournament_manager.integration;

import com.tournament.tournament_manager.TestcontainersConfiguration;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.TournamentRepository;
import com.tournament.tournament_manager.domain.port.in.maintenance.PurgeUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste la purge physique des entités soft-deleted.
 *
 * <p>Utilise {@link JdbcTemplate} pour vérifier la présence des enregistrements
 * directement en base (bypasse le {@code @SQLRestriction} des repositories JPA
 * qui masque les entités supprimées).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PurgeServiceIntegrationTest {

    @Autowired
    private PurgeUseCase purgeUseCase;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void purge_shouldPhysicallyDeletePlayersDeletedBeforeRetentionLimit() {
        // Joueur supprimé il y a 31 jours -> doit être purgé
        PlayerEntity oldDeleted = new PlayerEntity();
        oldDeleted.setUsername("old_deleted");
        oldDeleted.setEmail("old_deleted@mail.com");
        oldDeleted.setDeleted(true);
        oldDeleted.setDeletedAt(Instant.now().minus(Duration.ofDays(31)));
        playerRepository.save(oldDeleted);

        // Joueur supprimé il y a 1 jour -> doit être conservé
        PlayerEntity recentDeleted = new PlayerEntity();
        recentDeleted.setUsername("recent_deleted");
        recentDeleted.setEmail("recent_deleted@mail.com");
        recentDeleted.setDeleted(true);
        recentDeleted.setDeletedAt(Instant.now().minus(Duration.ofDays(1)));
        playerRepository.save(recentDeleted);

        // Joueur non supprimé -> doit être conservé
        PlayerEntity active = new PlayerEntity();
        active.setUsername("active");
        active.setEmail("active@mail.com");
        playerRepository.save(active);

        purgeUseCase.purgeDeletedEntities(30);

        // JdbcTemplate bypasse le @SQLRestriction pour voir toutes les lignes
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM players WHERE deleted = true", Integer.class);
        assertEquals(1, count, "Seul le joueur récemment supprimé doit rester");

        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM players WHERE deleted = false", Integer.class);
        assertEquals(1, activeCount, "Le joueur actif doit toujours être là");
    }

    @Test
    void purge_shouldPhysicallyDeleteTournamentsDeletedBeforeRetentionLimit() {
        TournamentEntity oldDeleted = new TournamentEntity();
        oldDeleted.setName("old_deleted_tournament");
        oldDeleted.setMaxPlayers(8);
        oldDeleted.setStatus(TournamentStatus.OPEN);
        oldDeleted.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        oldDeleted.setDeleted(true);
        oldDeleted.setDeletedAt(Instant.now().minus(Duration.ofDays(31)));
        tournamentRepository.save(oldDeleted);

        TournamentEntity recentDeleted = new TournamentEntity();
        recentDeleted.setName("recent_deleted_tournament");
        recentDeleted.setMaxPlayers(8);
        recentDeleted.setStatus(TournamentStatus.OPEN);
        recentDeleted.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        recentDeleted.setDeleted(true);
        recentDeleted.setDeletedAt(Instant.now().minus(Duration.ofDays(1)));
        tournamentRepository.save(recentDeleted);

        purgeUseCase.purgeDeletedEntities(30);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tournaments WHERE deleted = true", Integer.class);
        assertEquals(1, count, "Seul le tournoi récemment supprimé doit rester");
    }

    @Test
    void purge_shouldNotDeleteAnything_whenNoEntitiesOlderThanRetention() {
        PlayerEntity recentDeleted = new PlayerEntity();
        recentDeleted.setUsername("recent");
        recentDeleted.setEmail("recent@mail.com");
        recentDeleted.setDeleted(true);
        recentDeleted.setDeletedAt(Instant.now().minus(Duration.ofDays(5)));
        playerRepository.save(recentDeleted);

        purgeUseCase.purgeDeletedEntities(30);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM players WHERE deleted = true", Integer.class);
        assertEquals(1, count, "Rien ne doit être purgé");
    }

    @Test
    void purge_shouldAnonymize_notPhysicallyDelete_whenSoftDeletedPlayerHasMatchHistory() {
        // Reproduit exactement le bug de la revue : un joueur soft-deleted ayant joué un
        // match. Avant le correctif, ce test aurait fait planter purgeDeletedEntities()
        // avec une DataIntegrityViolationException (violation de contrainte FK sur
        // matches.player1_id), et plus rien n'aurait jamais été purgé.
        TournamentEntity tournament = new TournamentEntity();
        tournament.setName("tournoi-purge-test-" + System.nanoTime());
        tournament.setMaxPlayers(8);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournament.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        tournament = tournamentRepository.save(tournament);

        PlayerEntity playerWithHistory = new PlayerEntity();
        playerWithHistory.setUsername("has_history_" + System.nanoTime());
        playerWithHistory.setEmail("has_history_" + System.nanoTime() + "@mail.com");
        playerWithHistory.setDeleted(true);
        playerWithHistory.setDeletedAt(Instant.now().minus(Duration.ofDays(31)));
        playerWithHistory = playerRepository.save(playerWithHistory);

        PlayerEntity opponent = new PlayerEntity();
        opponent.setUsername("opponent_" + System.nanoTime());
        opponent.setEmail("opponent_" + System.nanoTime() + "@mail.com");
        opponent = playerRepository.save(opponent);

        jdbcTemplate.update(
                "INSERT INTO matches (round, tournament_id, player1_id, player2_id, winner_id, status) "
                        + "VALUES (?, ?, ?, ?, ?, 'FINISHED')",
                2, tournament.getId(), playerWithHistory.getId(), opponent.getId(), playerWithHistory.getId());

        Long playerId = playerWithHistory.getId();

        // Ne doit lever aucune exception (c'était le bug : DataIntegrityViolationException).
        purgeUseCase.purgeDeletedEntities(30);

        // Le joueur n'a pas été supprimé physiquement : la ligne existe toujours.
        Integer stillExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM players WHERE id = ?", Integer.class, playerId);
        assertEquals(1, stillExists, "Un joueur avec historique ne doit jamais être supprimé physiquement");

        // Mais il a bien été anonymisé.
        String username = jdbcTemplate.queryForObject(
                "SELECT username FROM players WHERE id = ?", String.class, playerId);
        assertTrue(username.startsWith("utilisateur-supprime-"), "Le username doit être anonymisé");

        Timestamp anonymizedAt = jdbcTemplate.queryForObject(
                "SELECT anonymized_at FROM players WHERE id = ?", Timestamp.class, playerId);
        assertNotNull(anonymizedAt, "anonymized_at doit être renseigné");

        // Le match référençant ce joueur est toujours intact.
        Integer matchStillExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM matches WHERE player1_id = ?", Integer.class, playerId);
        assertEquals(1, matchStillExists, "L'historique de match ne doit pas être touché");
    }

    @Test
    void purge_shouldNotReanonymize_playerAlreadyAnonymized() {
        PlayerEntity alreadyAnonymized = new PlayerEntity();
        alreadyAnonymized.setUsername("utilisateur-supprime-deja");
        alreadyAnonymized.setEmail("deja@anonymise.invalid");
        alreadyAnonymized.setDeleted(true);
        alreadyAnonymized.setDeletedAt(Instant.now().minus(Duration.ofDays(31)));
        alreadyAnonymized.setAnonymizedAt(Instant.now().minus(Duration.ofDays(20)));
        PlayerEntity saved = playerRepository.save(alreadyAnonymized);

        // Lui donner un historique pour qu'il resterait éligible à l'anonymisation
        // s'il n'était pas déjà marqué comme traité.
        TournamentEntity tournament = new TournamentEntity();
        tournament.setName("tournoi-purge-test2-" + System.nanoTime());
        tournament.setMaxPlayers(8);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournament.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        tournament = tournamentRepository.save(tournament);
        jdbcTemplate.update(
                "INSERT INTO matches (round, tournament_id, player1_id, status) VALUES (?, ?, ?, 'FINISHED')",
                2, tournament.getId(), saved.getId());

        Timestamp beforeSecondRun = jdbcTemplate.queryForObject(
                "SELECT anonymized_at FROM players WHERE id = ?", Timestamp.class, saved.getId());

        purgeUseCase.purgeDeletedEntities(30);

        Timestamp afterSecondRun = jdbcTemplate.queryForObject(
                "SELECT anonymized_at FROM players WHERE id = ?", Timestamp.class, saved.getId());

        assertEquals(beforeSecondRun, afterSecondRun,
                "Un joueur déjà anonymisé ne doit pas être retraité");
    }
}