package com.tournament.tournament_manager.application.bracket;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.tournament.ClaimRoundAdvancementPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvanceBracketServiceTest {

    @Mock
    private SaveTournamentPort saveTournamentPort;
    @Mock
    private LoadMatchByTournamentPort loadMatchByTournamentPort;
    @Mock
    private SaveMatchPort saveMatchPort;
    @Mock
    private ClaimRoundAdvancementPort claimRoundAdvancementPort;

    @InjectMocks
    private AdvanceBracketService advanceBracketService;

    @BeforeEach
    void setUp() {
        // Par défaut, la réclamation du round réussit (nominal case). Les tests spécifiques
        // à la garde d'idempotence écrasent ce stub avec `false`.
        lenient().when(claimRoundAdvancementPort.tryClaim(anyLong(), anyInt())).thenReturn(true);
    }

    @Test
    void advanceToNextRound_shouldDoNothing_whenNotAllMatchesFinished() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Match pendingMatch = new Match();
        pendingMatch.setStatus(MatchStatus.PENDING);
        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 4))
                .thenReturn(List.of(pendingMatch));
        advanceBracketService.advanceToNextRound(tournament, 4);
        verify(saveMatchPort, never()).saveMatch(any());
        verify(saveTournamentPort, never()).saveTournament(any());
        // Round non terminé : on ne doit même pas tenter de réclamer le round suivant.
        verify(claimRoundAdvancementPort, never()).tryClaim(anyLong(), anyInt());
    }

    @Test
    void advanceToNextRound_shouldFinishTournament_whenNextRoundLessThan2() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        Player winner = new Player();
        Match finishedMatch = new Match();
        finishedMatch.setStatus(MatchStatus.FINISHED);
        finishedMatch.setWinner(winner);
        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 2))
                .thenReturn(List.of(finishedMatch));
        advanceBracketService.advanceToNextRound(tournament, 2);
        assertEquals(TournamentStatus.FINISHED, tournament.getStatus());
        verify(saveTournamentPort, times(1)).saveTournament(tournament);
    }

    @Test
    void advanceToNextRound_shouldCreateNextRoundMatches_whenAllMatchesFinished() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Player winner1 = new Player();
        Player winner2 = new Player();
        Match match1 = new Match();
        match1.setStatus(MatchStatus.FINISHED);
        match1.setWinner(winner1);
        Match match2 = new Match();
        match2.setStatus(MatchStatus.FINISHED);
        match2.setWinner(winner2);
        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 4))
                .thenReturn(List.of(match1, match2));
        advanceBracketService.advanceToNextRound(tournament, 4);
        verify(saveMatchPort, times(1)).saveMatch(any(Match.class));
    }

    @Test
    void advanceToNextRound_shouldCreateByeMatch_whenOddNumberOfWinners() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Player winner1 = new Player();
        Player winner2 = new Player();
        Player winner3 = new Player();
        Match match1 = new Match();
        match1.setStatus(MatchStatus.FINISHED);
        match1.setWinner(winner1);
        Match match2 = new Match();
        match2.setStatus(MatchStatus.FINISHED);
        match2.setWinner(winner2);
        Match match3 = new Match();
        match3.setStatus(MatchStatus.FINISHED);
        match3.setWinner(winner3);
        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 8))
                .thenReturn(List.of(match1, match2, match3));
        advanceBracketService.advanceToNextRound(tournament, 8);
        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, atLeast(1)).saveMatch(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(m -> m.getPlayer2() == null));
    }

    @Test
    void advanceToNextRound_shouldCreateByeMatch_withThreeWinnersAtRound4() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);

        Player player1 = new Player();
        Player player2 = new Player();
        Player player3 = new Player();

        Match m1 = new Match();
        m1.setStatus(MatchStatus.FINISHED);
        m1.setWinner(player1);

        Match m2 = new Match();
        m2.setStatus(MatchStatus.FINISHED);
        m2.setWinner(player2);

        Match m3 = new Match();
        m3.setStatus(MatchStatus.FINISHED);
        m3.setWinner(player3);

        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 4))
                .thenReturn(List.of(m1, m2, m3));

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        advanceBracketService.advanceToNextRound(tournament, 4);

        verify(saveMatchPort, atLeast(1)).saveMatch(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(m -> m.getPlayer2() == null));
    }

    @Test
    void advanceToNextRound_shouldPairWinnersByPosition_notRandomly() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);

        Player w0 = new Player();
        w0.setId(10L);
        Player w1 = new Player();
        w1.setId(11L);
        Player w2 = new Player();
        w2.setId(12L);
        Player w3 = new Player();
        w3.setId(13L);

        Match m0 = new Match();
        m0.setPosition(0);
        m0.setStatus(MatchStatus.FINISHED);
        m0.setWinner(w0);
        Match m1 = new Match();
        m1.setPosition(1);
        m1.setStatus(MatchStatus.FINISHED);
        m1.setWinner(w1);
        Match m2 = new Match();
        m2.setPosition(2);
        m2.setStatus(MatchStatus.FINISHED);
        m2.setWinner(w2);
        Match m3 = new Match();
        m3.setPosition(3);
        m3.setStatus(MatchStatus.FINISHED);
        m3.setWinner(w3);

        // Volontairement dans le désordre : la méthode doit trier par position elle-même.
        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 8))
                .thenReturn(List.of(m3, m1, m0, m2));

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        advanceBracketService.advanceToNextRound(tournament, 8);
        verify(saveMatchPort, times(2)).saveMatch(captor.capture());

        // Position 0+1 -> nouvelle position 0 (w0 vs w1) ; position 2+3 -> nouvelle position 1 (w2 vs w3).
        Match nextMatch0 = captor.getAllValues().stream()
                .filter(m -> m.getPosition() == 0).findFirst().orElseThrow();
        Match nextMatch1 = captor.getAllValues().stream()
                .filter(m -> m.getPosition() == 1).findFirst().orElseThrow();

        assertEquals(w0, nextMatch0.getPlayer1());
        assertEquals(w1, nextMatch0.getPlayer2());
        assertEquals(w2, nextMatch1.getPlayer1());
        assertEquals(w3, nextMatch1.getPlayer2());
    }

    @Test
    void advanceToNextRound_shouldDoNothing_whenRoundAlreadyClaimed() {
        // Simule une redelivery Kafka du dernier match d'un round déjà traité : le round
        // suivant a déjà été réclamé (par le premier passage, ou par un appel concurrent).
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Player winner1 = new Player();
        Player winner2 = new Player();
        Match match1 = new Match();
        match1.setStatus(MatchStatus.FINISHED);
        match1.setWinner(winner1);
        Match match2 = new Match();
        match2.setStatus(MatchStatus.FINISHED);
        match2.setWinner(winner2);
        when(loadMatchByTournamentPort.loadByTournamentIdAndRound(1L, 4))
                .thenReturn(List.of(match1, match2));
        when(claimRoundAdvancementPort.tryClaim(1L, 2)).thenReturn(false);

        advanceBracketService.advanceToNextRound(tournament, 4);

        // Aucun match créé, aucun statut de tournoi modifié : no-op idempotent.
        verify(saveMatchPort, never()).saveMatch(any());
        verify(saveTournamentPort, never()).saveTournament(any());
    }
}
