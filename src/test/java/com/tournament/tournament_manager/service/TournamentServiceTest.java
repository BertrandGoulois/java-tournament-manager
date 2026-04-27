package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.dto.request.CreateTournamentRequest;
import com.tournament.tournament_manager.exception.InvalidTournamentException;
import com.tournament.tournament_manager.exception.TournamentAlreadyExistsException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import com.tournament.tournament_manager.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @InjectMocks
    private TournamentService tournamentService;

    @Test
    void createTournament_shouldThrow_whenNameAlreadyExists() {
        when(tournamentRepository.existsByName("Test")).thenReturn(true);
        assertThrows(TournamentAlreadyExistsException.class,
                () -> tournamentService.createTournament(new CreateTournamentRequest("Test", 4)));
    }

    @Test
    void createTournament_shouldThrow_whenMaxPlayersNotPowerOfTwo() {
        when(tournamentRepository.existsByName("Test")).thenReturn(false);
        assertThrows(InvalidTournamentException.class,
                () -> tournamentService.createTournament(new CreateTournamentRequest("Test", 3)));
    }

    @Test
    void createTournament_shouldReturnResponse_whenValid() {
        Tournament saved = new Tournament();
        saved.setName("Test");
        saved.setMaxPlayers(4);

        when(tournamentRepository.existsByName("Test")).thenReturn(false);
        when(tournamentRepository.save(any())).thenReturn(saved);

        var response = tournamentService.createTournament(new CreateTournamentRequest("Test", 4));
        assertEquals("Test", response.name());
    }

    @Test
    void getTournamentById_shouldThrow_whenNotFound() {
        when(tournamentRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(TournamentNotFoundException.class, () -> tournamentService.getTournamentById(1L));
    }
}