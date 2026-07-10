package com.tournament.tournament_manager.infrastructure.output.persistence.adapter;

import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationJpaAdapterTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private RegistrationJpaAdapter registrationJpaAdapter;

    @Test
    void loadByTournamentId_shouldReturnRegistrations() {
        Registration registration = new Registration();
        when(registrationRepository.findByTournamentId(1L)).thenReturn(List.of(registration));

        List<Registration> result = registrationJpaAdapter.loadByTournamentId(1L);

        assertEquals(1, result.size());
    }

    @Test
    void saveRegistration_shouldReturnSavedRegistration() {
        Registration registration = new Registration();
        when(registrationRepository.save(any())).thenReturn(registration);

        Registration result = registrationJpaAdapter.saveRegistration(registration);

        assertNotNull(result);
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