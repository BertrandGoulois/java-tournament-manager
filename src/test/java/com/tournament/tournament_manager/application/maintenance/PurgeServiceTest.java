package com.tournament.tournament_manager.application.maintenance;

import com.tournament.tournament_manager.domain.model.PurgeResult;
import com.tournament.tournament_manager.domain.port.out.maintenance.PurgeOutboxEventsPort;
import com.tournament.tournament_manager.domain.port.out.maintenance.PurgePlayersPort;
import com.tournament.tournament_manager.domain.port.out.maintenance.PurgeRefreshTokensPort;
import com.tournament.tournament_manager.domain.port.out.maintenance.PurgeTournamentsPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Contrairement à sa version précédente (voir le git log), ce service ne dépend plus que
 * de ports — il devient donc, pour la première fois, testable en isolation sans base de
 * données réelle. {@code PurgeServiceIntegrationTest} reste le test de référence pour le
 * comportement métier fin (anonymisation, contraintes FK réelles) ; celui-ci vérifie
 * seulement l'orchestration.
 */
@ExtendWith(MockitoExtension.class)
class PurgeServiceTest {

    @Mock
    private PurgePlayersPort purgePlayersPort;
    @Mock
    private PurgeTournamentsPort purgeTournamentsPort;
    @Mock
    private PurgeRefreshTokensPort purgeRefreshTokensPort;
    @Mock
    private PurgeOutboxEventsPort purgeOutboxEventsPort;

    @InjectMocks
    private PurgeService purgeService;

    @Test
    void purgeDeletedEntities_shouldCallAllPortsAndAggregateResult() {
        when(purgePlayersPort.anonymizeWithHistory(any())).thenReturn(2);
        when(purgePlayersPort.purgeWithoutHistory(any())).thenReturn(3);
        when(purgeTournamentsPort.purgeDeletedBefore(any())).thenReturn(1);
        when(purgeRefreshTokensPort.deleteExpiredBefore(any())).thenReturn(5);
        when(purgeOutboxEventsPort.deletePublishedBefore(any())).thenReturn(7);

        PurgeResult result = purgeService.purgeDeletedEntities(30);

        assertEquals(2, result.anonymizedPlayers());
        assertEquals(3, result.purgedPlayers());
        assertEquals(1, result.purgedTournaments());
        assertEquals(5, result.purgedRefreshTokens());
        assertEquals(7, result.purgedOutboxEvents());
    }

    @Test
    void purgeDeletedEntities_shouldUseRetentionDaysForSoftDeletedEntities_butNotForRefreshTokens() {
        when(purgePlayersPort.anonymizeWithHistory(any())).thenReturn(0);
        when(purgePlayersPort.purgeWithoutHistory(any())).thenReturn(0);
        when(purgeTournamentsPort.purgeDeletedBefore(any())).thenReturn(0);
        when(purgeRefreshTokensPort.deleteExpiredBefore(any())).thenReturn(0);
        when(purgeOutboxEventsPort.deletePublishedBefore(any())).thenReturn(0);

        Instant before = Instant.now();
        purgeService.purgeDeletedEntities(30);
        Instant after = Instant.now();

        ArgumentCaptor<Instant> retentionLimitCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(purgeTournamentsPort).purgeDeletedBefore(retentionLimitCaptor.capture());
        // La limite de rétention doit être ~30 jours dans le passé, pas "maintenant".
        assertEquals(true, retentionLimitCaptor.getValue().isBefore(before.minusSeconds(29 * 24 * 3600)));

        // Les refresh tokens expirés, eux, sont purgés par rapport à "maintenant", pas à la rétention.
        ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(purgeRefreshTokensPort).deleteExpiredBefore(nowCaptor.capture());
        assertEquals(true, !nowCaptor.getValue().isBefore(before) && !nowCaptor.getValue().isAfter(after));
    }
}
