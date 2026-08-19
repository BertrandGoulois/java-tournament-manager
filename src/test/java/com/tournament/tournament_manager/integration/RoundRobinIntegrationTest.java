package com.tournament.tournament_manager.integration;

import com.tournament.tournament_manager.TestcontainersConfiguration;
import com.tournament.tournament_manager.application.tournament.*;
import com.tournament.tournament_manager.domain.model.CreatePlayerCommand;
import com.tournament.tournament_manager.domain.model.CreateTournamentCommand;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.RecordMatchResultCommand;
import com.tournament.tournament_manager.domain.model.RegisterPlayerCommand;
import com.tournament.tournament_manager.domain.model.Standings;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.MatchEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.MatchRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.TournamentMapper;
import com.tournament.tournament_manager.application.match.RecordMatchResultService;
import com.tournament.tournament_manager.application.player.CreatePlayerService;
import com.tournament.tournament_manager.application.registration.RegisterPlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Teste le flux complet d'un tournoi round-robin : création, démarrage,
 * enregistrement de tous les résultats, et vérification du classement final
 * ainsi que du passage du tournoi à FINISHED.
 *
 * <p>Le déclenchement normal de {@link CheckTournamentCompletionService} passe
 * par {@code BracketListener} sur un événement Kafka asynchrone. Ce test
 * n'utilise pas de container Kafka (volontairement exclu de la CI, comme
 * {@code PlayerIntegrationTest} et {@code KafkaIntegrationTest}) : il appelle
 * directement le service pour valider la logique métier sur une vraie base
 * de données via Testcontainers, sans dépendre de l'infra de messaging.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoundRobinIntegrationTest {

    @Autowired
    private CreatePlayerService createPlayerService;
    @Autowired
    private CreateTournamentService createTournamentService;
    @Autowired
    private RegisterPlayerService registerPlayerService;
    @Autowired
    private StartTournamentService startTournamentService;
    @Autowired
    private RecordMatchResultService recordMatchResultService;
    @Autowired
    private CheckTournamentCompletionService checkTournamentCompletionService;
    @Autowired
    private GetStandingsService getStandingsService;
    @Autowired
    private GetTournamentService getTournamentService;
    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private TournamentMapper tournamentMapper;

    @Test
    void roundRobinTournament_shouldCompleteFullFlow() {
        // Création de 4 joueurs
        Player p1 = createPlayerService.createPlayer(new CreatePlayerCommand("rr_alice", "rr_alice@mail.com"));
        Player p2 = createPlayerService.createPlayer(new CreatePlayerCommand("rr_bob", "rr_bob@mail.com"));
        Player p3 = createPlayerService.createPlayer(new CreatePlayerCommand("rr_carol", "rr_carol@mail.com"));
        Player p4 = createPlayerService.createPlayer(new CreatePlayerCommand("rr_dave", "rr_dave@mail.com"));

        // Création du tournoi en round-robin
        Tournament tournament = createTournamentService.createTournament(
                new CreateTournamentCommand("RR Integration Cup", 4, TournamentFormat.ROUND_ROBIN, null, null));
        assertEquals(TournamentFormat.ROUND_ROBIN, tournament.getFormat());

        // Inscription des 4 joueurs
        registerPlayerService.registerPlayer(new RegisterPlayerCommand(p1.getId(), tournament.getId()));
        registerPlayerService.registerPlayer(new RegisterPlayerCommand(p2.getId(), tournament.getId()));
        registerPlayerService.registerPlayer(new RegisterPlayerCommand(p3.getId(), tournament.getId()));
        registerPlayerService.registerPlayer(new RegisterPlayerCommand(p4.getId(), tournament.getId()));

        // Démarrage : génère 6 matchs (C(4,2))
        startTournamentService.startTournament(tournament.getId());

        List<MatchEntity> matches = matchRepository.findByTournamentId(tournament.getId());
        assertEquals(6, matches.size());

        // Enregistrement de tous les résultats : chaque match est gagné par player1.
        // En production, chaque enregistrement déclenche un événement Kafka
        // qui appelle CheckTournamentCompletionService de façon asynchrone via BracketListener.
        // On simule cet appel directement ici (cf. Javadoc de la classe).
        for (MatchEntity match : matches) {
            recordMatchResultService.recordMatchResult(
                    match.getId(), new RecordMatchResultCommand(match.getPlayer1().getId()));
        }

        // On recharge le tournoi pour avoir l'entité à jour, puis on déclenche
        // manuellement la vérification de fin de tournoi (équivalent du listener Kafka).
        var allMatches = matchRepository.findByTournamentId(tournament.getId());
        var loadedTournament = tournamentMapper.toDomain(allMatches.get(0).getTournament());
        checkTournamentCompletionService.checkCompletion(loadedTournament);

        Tournament finished = getTournamentService.getTournamentById(tournament.getId());
        assertEquals(TournamentStatus.FINISHED, finished.getStatus());

        // Le classement doit refléter les victoires
        Standings standings = getStandingsService.getStandings(tournament.getId());
        assertEquals(4, standings.standings().size());
    }
}
