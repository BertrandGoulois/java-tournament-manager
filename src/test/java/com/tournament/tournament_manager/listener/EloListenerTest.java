package com.tournament.tournament_manager.listener;

import com.tournament.tournament_manager.domain.event.MatchFinishedEvent;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.exception.MatchNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.service.EloService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EloListenerTest {

    @Mock
    private EloService eloService;
    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private EloListener eloListener;

    @Test
    void onMatchFinished_shouldCallUpdateElo() {
        Match match = new Match();
        when(matchRepository.findById(1L)).thenReturn(Optional.of(match));

        eloListener.onMatchFinished(new MatchFinishedEvent(1L));

        verify(eloService, times(1)).updateElo(match);
    }

    @Test
    void onMatchFinished_shouldThrow_whenMatchNotFound() {
        when(matchRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(MatchNotFoundException.class,
                () -> eloListener.onMatchFinished(new MatchFinishedEvent(99L)));
    }
}