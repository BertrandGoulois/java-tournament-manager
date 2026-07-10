package com.tournament.tournament_manager.application.player;

import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.in.player.DeletePlayerUseCase;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import com.tournament.tournament_manager.domain.port.out.player.SoftDeletePlayerPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Cas d'utilisation : suppression (soft delete) d'un joueur.
 */
@Service
@Transactional
public class DeletePlayerService implements DeletePlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final SoftDeletePlayerPort softDeletePlayerPort;

    public DeletePlayerService(LoadPlayerPort loadPlayerPort,
                               SoftDeletePlayerPort softDeletePlayerPort) {
        this.loadPlayerPort = loadPlayerPort;
        this.softDeletePlayerPort = softDeletePlayerPort;
    }

    @Override
    public void deletePlayer(Long id) {
        Player player = loadPlayerPort.loadPlayer(id);
        player.setDeleted(true);
        player.setDeletedAt(LocalDateTime.now());
        softDeletePlayerPort.softDeletePlayer(player);
    }
}