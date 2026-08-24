package com.tournament.tournament_manager.infrastructure.input.scheduler;

import com.tournament.tournament_manager.domain.model.PurgeResult;
import com.tournament.tournament_manager.domain.port.in.maintenance.PurgeUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Vérifie que {@code PurgeScheduler} ne fait que déclencher le use case, sans porter la
 * moindre logique métier lui-même — c'est tout l'objet du point 24 de la revue.
 */
@ExtendWith(MockitoExtension.class)
class PurgeSchedulerTest {

    @Mock
    private PurgeUseCase purgeUseCase;

    @Test
    void purgeDeletedEntities_shouldDelegateToUseCase_withConfiguredRetentionDays() {
        PurgeScheduler scheduler = new PurgeScheduler(purgeUseCase);
        ReflectionTestUtils.setField(scheduler, "retentionDays", 45);

        when(purgeUseCase.purgeDeletedEntities(45))
                .thenReturn(new PurgeResult(1, 2, 3, 4, 5));

        scheduler.purgeDeletedEntities();

        verify(purgeUseCase, times(1)).purgeDeletedEntities(eq(45));
    }
}
