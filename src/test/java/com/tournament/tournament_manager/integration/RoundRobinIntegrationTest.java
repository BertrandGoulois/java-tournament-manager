package com.tournament.tournament_manager.integration;

import com.tournament.tournament_manager.TestcontainersConfiguration;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.dto.request.match.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.request.registration.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.request.tournament.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.match.MatchResponse;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.dto.response.tournament.StandingsResponse;
import com.tournament.tournament_manager.dto.response.tournament.TournamentResponse;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.service.match.RecordMatchResultService;
import com.tournament.tournament_manager.service.player.CreatePlayerService;
import com.tournament.tournament_manager.service.registration.RegisterPlayerService;
import com.tournament.tournament_manager.service.tournament.CheckTournamentCompletionService;
import com.tournament.tournament_manager.service.tournament.CreateTournamentService;
import com.tournament.tournament_manager.service.tournament.GetStandingsService;
import com.tournament.tournament_manager.service.tournament.GetTournamentService;
import com.tournament.tournament_manager.service.tournament.StartTournamentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void roundRobinTournament_shouldCompleteFullFlow() {
        // Création de 4 joueurs
        PlayerResponse p1 = createPlayerService.createPlayer(new CreatePlayerRequest("rr_alice", "rr_alice@mail.com"));
        PlayerResponse p2 = createPlayerService.createPlayer(new CreatePlayerRequest("rr_bob", "rr_bob@mail.com"));
        PlayerResponse p3 = createPlayerService.createPlayer(new CreatePlayerRequest("rr_carol", "rr_carol@mail.com"));
        PlayerResponse p4 = createPlayerService.createPlayer(new CreatePlayerRequest("rr_dave", "rr_dave@mail.com"));

        // Création du tournoi en round-robin
        TournamentResponse tournament = createTournamentService.createTournament(
                new CreateTournamentRequest("RR Integration Cup", 4, TournamentFormat.ROUND_ROBIN, null, null));
        assertEquals(TournamentFormat.ROUND_ROBIN, tournament.format());

        // Inscription des 4 joueurs
        registerPlayerService.registerPlayer(new CreateRegistrationRequest(p1.id(), tournament.id()));
        registerPlayerService.registerPlayer(new CreateRegistrationRequest(p2.id(), tournament.id()));
        registerPlayerService.registerPlayer(new CreateRegistrationRequest(p3.id(), tournament.id()));
        registerPlayerService.registerPlayer(new CreateRegistrationRequest(p4.id(), tournament.id()));

        // Démarrage : génère 6 matchs (C(4,2))
        startTournamentService.startTournament(tournament.id());

        List<MatchResponse> matches = matchRepository.findByTournamentId(tournament.id()).stream()
                .map(m -> new MatchResponse(m.getId(), m.getRound(), m.getStatus(), m.getPlayedAt(),
                        m.getTournament().getId(), m.getPlayer1().getId(),
                        m.getPlayer2() != null ? m.getPlayer2().getId() : null,
                        m.getWinner() != null ? m.getWinner().getId() : null))
                .toList();
        assertEquals(6, matches.size());

        // Enregistrement de tous les résultats : chaque match est gagné par player1.
        // En production, chaque enregistrement déclenche un événement Kafka
        // qui appelle CheckTournamentCompletionService de façon asynchrone via BracketListener.
        // On simule cet appel directement ici (cf. Javadoc de la classe).
        for (MatchResponse match : matches) {
            recordMatchResultService.recordMatchResult(match.id(), new RecordMatchResultRequest(match.player1Id()));
        }

        // On recharge le tournoi pour avoir l'entité à jour, puis on déclenche
        // manuellement la vérification de fin de tournoi (équivalent du listener Kafka).
        var allMatches = matchRepository.findByTournamentId(tournament.id());
        var loadedTournament = allMatches.get(0).getTournament();
        checkTournamentCompletionService.checkCompletion(loadedTournament);

        TournamentResponse finished = getTournamentService.getTournamentById(tournament.id());
        assertEquals(TournamentStatus.FINISHED, finished.status());

        // Le classement doit refléter les victoires
        StandingsResponse standings = getStandingsService.getStandings(tournament.id());
        assertEquals(4, standings.standings().size());
    }
}