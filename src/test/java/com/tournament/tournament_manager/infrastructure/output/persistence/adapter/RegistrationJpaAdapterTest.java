package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.RegistrationEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.PlayerMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.RegistrationMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.mapper.TournamentMapper;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.RegistrationRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.TournamentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;

@ExtendWith(MockitoExtension.class)
class RegistrationJpaAdapterTest {

    @Mock
    private RegistrationRepository registrationRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private TournamentRepository tournamentRepository;

    private RegistrationJpaAdapter registrationJpaAdapter;

    @BeforeEach
    void setUp() {
        PlayerMapper playerMapper = new PlayerMapper();
        TournamentMapper tournamentMapper = new TournamentMapper();
        registrationJpaAdapter = new RegistrationJpaAdapter(
                registrationRepository, playerRepository, tournamentRepository,
                new RegistrationMapper(playerMapper, tournamentMapper));
    }

    @Test
    void loadByTournamentId_shouldReturnRegistrations() {
        RegistrationEntity entity = new RegistrationEntity();
        entity.setId(1L);
        when(registrationRepository.findByTournamentId(1L)).thenReturn(List.of(entity));

        List<Registration> result = registrationJpaAdapter.loadByTournamentId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void saveRegistration_shouldResolveReferencesAndReturnSaved() {
        Tournament tournament = Tournament.reconstitute(10L, new TournamentName("Test Tournament"), TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, null, null, 0, null, false, null);
        Player player = new Player();
        player.setId(1L);

        Registration registration = new Registration();
        registration.setTournament(tournament);
        registration.setPlayer(player);

        TournamentEntity tournamentEntityRef = new TournamentEntity();
        tournamentEntityRef.setName("Test Tournament");
        when(tournamentRepository.getReferenceById(10L)).thenReturn(tournamentEntityRef);
        when(playerRepository.getReferenceById(1L)).thenReturn(new PlayerEntity());
        when(registrationRepository.save(any())).thenAnswer(inv -> {
            RegistrationEntity e = inv.getArgument(0);
            e.setId(42L);
            return e;
        });

        Registration result = registrationJpaAdapter.saveRegistration(registration);

        assertNotNull(result);
        assertEquals(42L, result.getId());
    }

    @Test
    void existsByPlayerIdAndTournamentId_shouldReturnTrue_whenExists() {
        when(registrationRepository.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(true);

        assertTrue(registrationJpaAdapter.existsByPlayerIdAndTournamentId(1L, 1L));
    }

    @Test
    void existsByPlayerIdAndTournamentId_shouldReturnFalse_whenNotExists() {
        when(registrationRepository.existsByPlayerIdAndTournamentId(1L, 1L)).thenReturn(false);

        assertFalse(registrationJpaAdapter.existsByPlayerIdAndTournamentId(1L, 1L));
    }

    @Test
    void countByTournamentId_shouldReturnCount() {
        when(registrationRepository.countByTournamentId(1L)).thenReturn(4L);

        assertEquals(4L, registrationJpaAdapter.countByTournamentId(1L));
    }
}
