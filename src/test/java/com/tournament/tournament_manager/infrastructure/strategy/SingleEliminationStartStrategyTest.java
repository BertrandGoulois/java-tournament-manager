package com.tournament.tournament_manager.infrastructure.strategy;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SingleEliminationStartStrategyTest {

    @Mock
    private SaveMatchPort saveMatchPort;

    @InjectMocks
    private SingleEliminationStartStrategy strategy;

    @Test
    void supportedFormat_shouldReturnSingleElimination() {
        assertEquals(TournamentFormat.SINGLE_ELIMINATION, strategy.supportedFormat());
    }

    @Test
    void generateInitialMatches_shouldCreateOneMatch_whenTwoPlayers() {
        Tournament tournament = new Tournament();
        List<Player> players = new ArrayList<>(List.of(new Player(), new Player()));

        strategy.generateInitialMatches(tournament, players);

        verify(saveMatchPort, times(1)).saveMatch(any(Match.class));
    }

    @Test
    void generateInitialMatches_shouldCreateTwoMatches_whenFourPlayers() {
        Tournament tournament = new Tournament();
        List<Player> players = new ArrayList<>(List.of(new Player(), new Player(), new Player(), new Player()));

        strategy.generateInitialMatches(tournament, players);

        verify(saveMatchPort, times(2)).saveMatch(any(Match.class));
    }

    @Test
    void generateInitialMatches_shouldCreateByeMatch_whenOddNumberOfPlayers() {
        Tournament tournament = new Tournament();
        List<Player> players = new ArrayList<>(List.of(new Player(), new Player(), new Player()));

        strategy.generateInitialMatches(tournament, players);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(2)).saveMatch(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(m -> m.getPlayer2() == null));
    }
}