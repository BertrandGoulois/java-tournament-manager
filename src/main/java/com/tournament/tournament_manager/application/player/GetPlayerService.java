package com.tournament.tournament_manager.application.player;

import com.tournament.tournament_manager.domain.model.PageRequest;
import com.tournament.tournament_manager.domain.model.PageResult;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerUseCase;
import com.tournament.tournament_manager.domain.port.out.player.LoadAllPlayersPort;
import com.tournament.tournament_manager.domain.port.out.player.LoadPlayerPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cas d'utilisation : consultation d'un ou plusieurs joueurs.
 *
 * <p>Retourne des objets de domaine purs — la conversion vers un format de réponse
 * (JSON REST, JSON-RPC...) est la responsabilité de l'adaptateur d'entrée appelant, jamais
 * de ce service (voir point 22 de la revue : un port ne doit pas parler le langage d'un
 * transport particulier).
 */
@Service
@Transactional(readOnly = true)
public class GetPlayerService implements GetPlayerUseCase {

    private final LoadPlayerPort loadPlayerPort;
    private final LoadAllPlayersPort loadAllPlayersPort;

    public GetPlayerService(LoadPlayerPort loadPlayerPort,
                            LoadAllPlayersPort loadAllPlayersPort) {
        this.loadPlayerPort = loadPlayerPort;
        this.loadAllPlayersPort = loadAllPlayersPort;
    }

    @Override
    public Player getPlayerById(Long id) {
        return loadPlayerPort.loadPlayer(id);
    }

    @Override
    public PageResult<Player> getAllPlayers(PageRequest pageRequest) {
        return loadAllPlayersPort.loadAllPlayers(pageRequest);
    }
}
