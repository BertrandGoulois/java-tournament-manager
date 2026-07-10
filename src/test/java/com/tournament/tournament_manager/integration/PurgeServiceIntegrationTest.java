package com.tournament.tournament_manager.integration;

import com.tournament.tournament_manager.TestcontainersConfiguration;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.repository.PlayerRepository;
import com.tournament.tournament_manager.repository.TournamentRepository;
import com.tournament.tournament_manager.service.PurgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private PurgeService purgeService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void purge_shouldPhysicallyDeletePlayersDeletedBeforeRetentionLimit() {
        // Joueur supprimé il y a 31 jours -> doit être purgé
        Player oldDeleted = new Player();
        oldDeleted.setUsername("old_deleted");
        oldDeleted.setEmail("old_deleted@mail.com");
        oldDeleted.setDeleted(true);
        oldDeleted.setDeletedAt(LocalDateTime.now().minusDays(31));
        playerRepository.save(oldDeleted);

        // Joueur supprimé il y a 1 jour -> doit être conservé
        Player recentDeleted = new Player();
        recentDeleted.setUsername("recent_deleted");
        recentDeleted.setEmail("recent_deleted@mail.com");
        recentDeleted.setDeleted(true);
        recentDeleted.setDeletedAt(LocalDateTime.now().minusDays(1));
        playerRepository.save(recentDeleted);

        // Joueur non supprimé -> doit être conservé
        Player active = new Player();
        active.setUsername("active");
        active.setEmail("active@mail.com");
        playerRepository.save(active);

        purgeService.purgeDeletedEntities();

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
        Tournament oldDeleted = new Tournament();
        oldDeleted.setName("old_deleted_tournament");
        oldDeleted.setMaxPlayers(8);
        oldDeleted.setStatus(TournamentStatus.OPEN);
        oldDeleted.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        oldDeleted.setDeleted(true);
        oldDeleted.setDeletedAt(LocalDateTime.now().minusDays(31));
        tournamentRepository.save(oldDeleted);

        Tournament recentDeleted = new Tournament();
        recentDeleted.setName("recent_deleted_tournament");
        recentDeleted.setMaxPlayers(8);
        recentDeleted.setStatus(TournamentStatus.OPEN);
        recentDeleted.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        recentDeleted.setDeleted(true);
        recentDeleted.setDeletedAt(LocalDateTime.now().minusDays(1));
        tournamentRepository.save(recentDeleted);

        purgeService.purgeDeletedEntities();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tournaments WHERE deleted = true", Integer.class);
        assertEquals(1, count, "Seul le tournoi récemment supprimé doit rester");
    }

    @Test
    void purge_shouldNotDeleteAnything_whenNoEntitiesOlderThanRetention() {
        Player recentDeleted = new Player();
        recentDeleted.setUsername("recent");
        recentDeleted.setEmail("recent@mail.com");
        recentDeleted.setDeleted(true);
        recentDeleted.setDeletedAt(LocalDateTime.now().minusDays(5));
        playerRepository.save(recentDeleted);

        purgeService.purgeDeletedEntities();

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM players WHERE deleted = true", Integer.class);
        assertEquals(1, count, "Rien ne doit être purgé");
    }
}