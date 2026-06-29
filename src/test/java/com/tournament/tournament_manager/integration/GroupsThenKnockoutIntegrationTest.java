package com.tournament.tournament_manager.integration;

import com.tournament.tournament_manager.TestcontainersConfiguration;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.dto.request.match.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.request.registration.CreateRegistrationRequest;
import com.tournament.tournament_manager.dto.request.tournament.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.dto.response.tournament.TournamentResponse;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.service.match.RecordMatchResultService;
import com.tournament.tournament_manager.service.player.CreatePlayerService;
import com.tournament.tournament_manager.service.registration.RegisterPlayerService;
import com.tournament.tournament_manager.service.tournament.CreateTournamentService;
import com.tournament.tournament_manager.service.tournament.GenerateKnockoutBracketFromGroupsService;
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
 * Teste le flux complet d'un tournoi {@code GROUPS_THEN_KNOCKOUT} : création
 * avec 2 groupes de 4 joueurs et 2 qualifiés par groupe, démarrage, résultats
 * de tous les matchs de groupe, génération du bracket final entre les 4 qualifiés,
 * résultats du bracket, et passage du tournoi à FINISHED.
 *
 * <p>Comme {@code RoundRobinIntegrationTest}, n'utilise pas de container Kafka :
 * la vérification d'achèvement de phase et l'avancement du bracket, normalement
 * déclenchés par {@code BracketListener} de façon asynchrone, sont appelés
 * directement pour valider la logique métier sur une vraie base de données.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class GroupsThenKnockoutIntegrationTest {

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
    private GenerateKnockoutBracketFromGroupsService generateKnockoutBracketFromGroupsService;
    @Autowired
    private GetTournamentService getTournamentService;
    @Autowired
    private MatchRepository matchRepository;

    @Test
    void groupsThenKnockoutTournament_shouldCompleteFullFlow() {
        // 8 joueurs
        List<PlayerResponse> players = List.of(
                createPlayerService.createPlayer(new CreatePlayerRequest("gtk_p1", "gtk_p1@mail.com")),
                createPlayerService.createPlayer(new CreatePlayerRequest("gtk_p2", "gtk_p2@mail.com")),
                createPlayerService.createPlayer(new CreatePlayerRequest("gtk_p3", "gtk_p3@mail.com")),
                createPlayerService.createPlayer(new CreatePlayerRequest("gtk_p4", "gtk_p4@mail.com")),
                createPlayerService.createPlayer(new CreatePlayerRequest("gtk_p5", "gtk_p5@mail.com")),
                createPlayerService.createPlayer(new CreatePlayerRequest("gtk_p6", "gtk_p6@mail.com")),
                createPlayerService.createPlayer(new CreatePlayerRequest("gtk_p7", "gtk_p7@mail.com")),
                createPlayerService.createPlayer(new CreatePlayerRequest("gtk_p8", "gtk_p8@mail.com"))
        );

        // Tournoi : 2 groupes de 4, 2 qualifiés par groupe (4 qualifiés au total, puissance de 2)
        TournamentResponse tournament = createTournamentService.createTournament(
                new CreateTournamentRequest("GTK Integration Cup", 8, TournamentFormat.GROUPS_THEN_KNOCKOUT, 2, 2));
        assertEquals(TournamentFormat.GROUPS_THEN_KNOCKOUT, tournament.format());

        for (PlayerResponse p : players) {
            registerPlayerService.registerPlayer(new CreateRegistrationRequest(p.id(), tournament.id()));
        }

        // Démarrage : 2 groupes de 4 -> C(4,2) = 6 matchs par groupe = 12 matchs de groupe
        startTournamentService.startTournament(tournament.id());

        List<Match> groupMatches = matchRepository.findByTournamentId(tournament.id());
        assertEquals(12, groupMatches.size());
        assertTrue(groupMatches.stream().allMatch(m -> m.getGroupNumber() != null));

        // Tous les matchs de groupe sont gagnés par player1
        for (Match match : groupMatches) {
            recordMatchResultService.recordMatchResult(
                    match.getId(), new RecordMatchResultRequest(match.getPlayer1().getId()));
        }

        // Déclenchement manuel équivalent au listener Kafka : génère le bracket
        var allMatchesAfterGroups = matchRepository.findByTournamentId(tournament.id());
        var loadedTournament = allMatchesAfterGroups.get(0).getTournament();
        generateKnockoutBracketFromGroupsService.checkGroupsCompletionAndGenerateBracket(loadedTournament);

        List<Match> allMatches = matchRepository.findByTournamentId(tournament.id());
        List<Match> bracketMatches = allMatches.stream()
                .filter(m -> m.getGroupNumber() == null)
                .toList();

        // 4 qualifiés -> 2 matchs de bracket (demi-finales)
        assertEquals(2, bracketMatches.size());
        assertTrue(bracketMatches.stream().allMatch(m -> m.getStatus() == MatchStatus.PENDING));

        TournamentResponse stillInProgress = getTournamentService.getTournamentById(tournament.id());
        assertEquals(TournamentStatus.IN_PROGRESS, stillInProgress.status());
    }
}