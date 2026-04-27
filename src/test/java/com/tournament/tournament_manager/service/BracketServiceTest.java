package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.NotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.repository.RegistrationRepository;
import com.tournament.tournament_manager.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private BracketService bracketService;

    @Test
    void startTournament_shouldThrowException_whenTournamentNotOpen() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.IN_PROGRESS);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        assertThrows(InvalidException.class, () -> bracketService.startTournament(1L));
    }

    @Test
    void startTournament_shouldThrowException_whenLessThanTwoPlayers() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));

        Registration registration = new Registration();
        registration.setPlayer(new Player());

        when(registrationRepository.findByTournamentId(1L)).thenReturn(List.of(registration));

        assertThrows(InvalidException.class, () -> bracketService.startTournament(1L));
    }

    @Test
    void startTournament_shouldThrowException_whenTournamentDoesntExist() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> bracketService.startTournament(1L));
    }

    @Test
    void startTournament_shouldCreateOneMatch_whenTwoPlayersRegistered() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);

        Registration reg1 = new Registration();
        reg1.setPlayer(new Player());
        Registration reg2 = new Registration();
        reg2.setPlayer(new Player());

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByTournamentId(1L)).thenReturn(List.of(reg1, reg2));

        bracketService.startTournament(1L);

        verify(matchRepository, times(1)).save(any(Match.class));
    }

    @Test
    void startTournament_shouldCreateTwoMatches_whenFourPlayersRegistered() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);

        List<Registration> registrations = List.of(
                registrationWithPlayer(), registrationWithPlayer(),
                registrationWithPlayer(), registrationWithPlayer()
        );

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByTournamentId(1L)).thenReturn(registrations);

        bracketService.startTournament(1L);

        verify(matchRepository, times(2)).save(any(Match.class));
    }

    @Test
    void startTournament_shouldSetTournamentInProgress_whenStarted() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);

        Registration reg1 = new Registration();
        reg1.setPlayer(new Player());
        Registration reg2 = new Registration();
        reg2.setPlayer(new Player());

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByTournamentId(1L)).thenReturn(List.of(reg1, reg2));

        bracketService.startTournament(1L);

        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(tournamentRepository, times(1)).save(tournament);
    }

    @Test
    void startTournament_shouldCreateByeMatch_whenOddNumberOfPlayers() {
        Tournament tournament = new Tournament();
        tournament.setStatus(TournamentStatus.OPEN);
        tournament.setMaxPlayers(4);

        List<Registration> registrations = List.of(
                registrationWithPlayer(), registrationWithPlayer(), registrationWithPlayer()
        );

        when(tournamentRepository.findById(1L)).thenReturn(Optional.of(tournament));
        when(registrationRepository.findByTournamentId(1L)).thenReturn(registrations);

        bracketService.startTournament(1L);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(matchRepository, times(2)).save(captor.capture());

        List<Match> savedMatches = captor.getAllValues();
        boolean hasByeMatch = savedMatches.stream().anyMatch(m -> m.getPlayer2() == null);
        assertTrue(hasByeMatch);
    }

    private Registration registrationWithPlayer() {
        Registration reg = new Registration();
        reg.setPlayer(new Player());
        return reg;
    }
}