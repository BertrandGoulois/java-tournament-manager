package com.tournament.tournament_manager.application.elo;

import com.tournament.tournament_manager.domain.model.EloHistory;
import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.valueobjects.EloRating;
import com.tournament.tournament_manager.domain.port.out.elo.SaveAllPlayersPort;
import com.tournament.tournament_manager.domain.port.out.elo.SaveEloHistoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;

@ExtendWith(MockitoExtension.class)
class EloServiceTest {

    @Mock
    private SaveAllPlayersPort saveAllPlayersPort;
    @Mock
    private SaveEloHistoryPort saveEloHistoryPort;

    @InjectMocks
    private EloService eloService;

    @BeforeEach
    void setUp() {
        // @Value n'est jamais resolu par @InjectMocks (pas de contexte Spring ici) - sans
        // ceci, kFactor vaudrait 0 (valeur par defaut du type primitif), et tous les
        // calculs ELO attendus dans ce fichier (bases sur K=32) seraient faux.
        ReflectionTestUtils.setField(eloService, "kFactor", 32);
    }

    /**
     * Deux joueurs ELO 1000 : expected = 0.5
     * newEloWinner = 1000 + 32 * (1 - 0.5) = 1016
     * newEloLoser  = 1000 + 32 * (0 - 0.5) = 984
     */
    @Test
    void updateElo_shouldCalculateExactValues_whenEqualRating() {
        Player winner = new Player();
        winner.setEloRating(new EloRating(1000));
        Player loser = new Player();
        loser.setEloRating(new EloRating(1000));

        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, winner, loser, winner);

        eloService.updateElo(match);

        assertEquals(1016, winner.getEloRating().value());
        assertEquals(984, loser.getEloRating().value());
    }

    /**
     * Winner ELO 1000, Loser ELO 1400
     * expectedWinner = 1 / (1 + 10^((1400-1000)/400)) = 1 / (1 + 10) = 0.0909...
     * newEloWinner = 1000 + 32 * (1 - 0.0909) = 1000 + 29.09 = 1029
     * newEloLoser  = 1400 + 32 * (0 - 0.9090) = 1400 - 29.09 = 1371
     */
    @Test
    void updateElo_shouldGiveMoreElo_whenBeatingStrongerOpponent() {
        Player winner = new Player();
        winner.setEloRating(new EloRating(1000));
        Player loser = new Player();
        loser.setEloRating(new EloRating(1400));

        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, winner, loser, winner);

        eloService.updateElo(match);

        assertEquals(1029, winner.getEloRating().value());
        assertEquals(1371, loser.getEloRating().value());
    }

    /**
     * Winner ELO 1400, Loser ELO 1000
     * expectedWinner = 1 / (1 + 10^((1000-1400)/400)) = 1 / (1 + 0.0909) = 0.9166...
     * newEloWinner = 1400 + 32 * (1 - 0.9166) = 1400 + 2.67 = 1403
     * newEloLoser  = 1000 + 32 * (0 - 0.0833) = 1000 - 2.67 = 997
     */
    @Test
    void updateElo_shouldGiveLessElo_whenBeatingWeakerOpponent() {
        Player winner = new Player();
        winner.setEloRating(new EloRating(1400));
        Player loser = new Player();
        loser.setEloRating(new EloRating(1000));

        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, winner, loser, winner);

        eloService.updateElo(match);

        assertEquals(1403, winner.getEloRating().value());
        assertEquals(997, loser.getEloRating().value());
    }

    @Test
    void updateElo_shouldCalculateExactValues_whenPlayer2IsWinner() {
        Player player1 = new Player();
        player1.setEloRating(new EloRating(1000));
        Player player2 = new Player();
        player2.setEloRating(new EloRating(1000));

        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, player1, player2, player2);

        eloService.updateElo(match);

        assertEquals(984, player1.getEloRating().value());
        assertEquals(1016, player2.getEloRating().value());
    }

    @Test
    void updateElo_shouldSaveBothPlayers() {
        Player winner = new Player();
        winner.setEloRating(new EloRating(1000));
        Player loser = new Player();
        loser.setEloRating(new EloRating(1000));

        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, winner, loser, winner);

        eloService.updateElo(match);

        verify(saveAllPlayersPort, times(1)).saveAllPlayers(anyList());
    }

    @Test
    void updateElo_shouldCreateTwoEloHistoryEntries() {
        Player winner = new Player();
        winner.setEloRating(new EloRating(1000));
        Player loser = new Player();
        loser.setEloRating(new EloRating(1000));

        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, winner, loser, winner);

        eloService.updateElo(match);

        verify(saveEloHistoryPort, times(2)).saveEloHistory(any());
    }

    @Test
    void updateElo_shouldSaveCorrectEloHistory() {
        Player winner = new Player();
        winner.setEloRating(new EloRating(1000));
        Player loser = new Player();
        loser.setEloRating(new EloRating(1000));

        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, winner, loser, winner);

        ArgumentCaptor<EloHistory> captor =
                ArgumentCaptor.forClass(EloHistory.class);

        eloService.updateElo(match);

        verify(saveEloHistoryPort, times(2)).saveEloHistory(captor.capture());
        List<EloHistory> histories = captor.getAllValues();

        // Premier appel = winner
        assertEquals(16, histories.get(0).getEloChange());
        assertEquals(1016, histories.get(0).getEloAfter());

        // Deuxième appel = loser
        assertEquals(-16, histories.get(1).getEloChange());
        assertEquals(984, histories.get(1).getEloAfter());
    }

    @Test
    void updateElo_shouldSaveEloHistoryWithCorrectPlayerAndMatch() {
        Player winner = new Player();
        winner.setEloRating(new EloRating(1000));
        Player loser = new Player();
        loser.setEloRating(new EloRating(1000));

        Match match = Match.reconstitute(null, 0, 0, null, MatchStatus.PENDING, null, null, null, winner, loser, winner);

        ArgumentCaptor<EloHistory> captor =
                ArgumentCaptor.forClass(EloHistory.class);

        eloService.updateElo(match);

        verify(saveEloHistoryPort, times(2)).saveEloHistory(captor.capture());
        List<EloHistory> histories = captor.getAllValues();

        assertEquals(winner, histories.get(0).getPlayer());
        assertEquals(match, histories.get(0).getMatch());
        assertEquals(loser, histories.get(1).getPlayer());
        assertEquals(match, histories.get(1).getMatch());
    }

    @Test
    void updateElo_shouldNotThrow_whenConcurrentExecutionAlreadyInsertedHistory() {
        // Simule la contrainte UNIQUE(match_id, player_id) violée par une exécution
        // concurrente ayant déjà inséré l'historique entre le check d'EloListener et cet
        // appel — ne doit pas planter le listener, juste être rattrapé silencieusement.
        Player winner = new Player();
        winner.setEloRating(new EloRating(1000));
        Player loser = new Player();
        loser.setEloRating(new EloRating(1000));

        Match match = Match.reconstitute(1L, 0, 0, null, MatchStatus.PENDING, null, null, null, winner, loser, winner);

        doThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"))
                .when(saveEloHistoryPort).saveEloHistory(any());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> eloService.updateElo(match));
    }
}