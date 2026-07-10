package com.tournament.tournament_manager.application.elo;

import com.tournament.tournament_manager.domain.model.entities.EloHistory;
import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.port.in.elo.UpdateEloUseCase;
import com.tournament.tournament_manager.domain.port.out.elo.SaveAllPlayersPort;
import com.tournament.tournament_manager.domain.port.out.elo.SaveEloHistoryPort;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Calcule et applique les variations de classement ELO après un match.
 *
 * <p>Utilise la formule ELO standard avec K=32 :
 * le gain/perte de points dépend de l'écart de classement entre les deux joueurs.
 * Battre un adversaire mieux classé rapporte plus de points que battre un outsider.
 *
 * <p>Invalide le cache Redis {@code playerStats} pour les deux joueurs après mise à jour.
 * <p>Dépend uniquement de ports (interfaces) - aucune dépendance directe vers JPA.
 */
@Service
@Transactional(readOnly = true)
public class EloService implements UpdateEloUseCase {

    private final SaveAllPlayersPort saveAllPlayersPort;
    private final SaveEloHistoryPort saveEloHistoryPort;

    public EloService(SaveAllPlayersPort saveAllPlayersPort,
                      SaveEloHistoryPort saveEloHistoryPort) {
        this.saveAllPlayersPort = saveAllPlayersPort;
        this.saveEloHistoryPort = saveEloHistoryPort;
    }

    /**
     * Met à jour le classement ELO des deux joueurs d'un match terminé
     * et persiste l'historique correspondant.
     *
     * <p>Le perdant est déduit par élimination : c'est le joueur parmi
     * {@code player1} et {@code player2} qui n'est pas le vainqueur.
     * Le résultat ELO est plafonné à {@code 0} (un ELO ne peut pas être négatif).
     *
     * @param match le match terminé, avec {@code winner} renseigné et {@code player2} non null
     */
    @Override
    @Caching(evict = {
            @CacheEvict(value = "playerStats", key = "#match.player1.id"),
            @CacheEvict(value = "playerStats", key = "#match.player2.id")
    })
    @Transactional
    public void updateElo(Match match) {
        Player winner = match.getWinner();
        Player loser = match.getPlayer1().equals(match.getWinner())
                ? match.getPlayer2()
                : match.getPlayer1();

        int eloWinner = winner.getEloRating().value();
        int eloLoser = loser.getEloRating().value();

        double expectedWinner = 1.0 / (1 + Math.pow(10, (eloLoser - eloWinner) / 400.0));
        double expectedLoser = 1.0 - expectedWinner;

        int K = 32;
        int newEloWinner = (int) Math.round(eloWinner + K * (1 - expectedWinner));
        int newEloLoser = (int) Math.round(eloLoser + K * (0 - expectedLoser));

        winner.setEloRating(winner.getEloRating().add(newEloWinner - eloWinner));
        loser.setEloRating(loser.getEloRating().add(newEloLoser - eloLoser));

        saveAllPlayersPort.saveAllPlayers(List.of(winner, loser));

        saveEloHistory(winner, match, newEloWinner - eloWinner, newEloWinner);
        saveEloHistory(loser, match, newEloLoser - eloLoser, newEloLoser);
    }

    /**
     * Crée et persiste une entrée d'historique ELO pour un joueur.
     *
     * @param player    le joueur concerné
     * @param match     le match à l'origine de la variation
     * @param eloChange la variation de classement (positive pour le vainqueur,
     *                  négative pour le perdant)
     * @param eloAfter  le classement du joueur après application de la variation
     */
    private void saveEloHistory(Player player, Match match, int eloChange, int eloAfter) {
        EloHistory history = new EloHistory();
        history.setPlayer(player);
        history.setMatch(match);
        history.setEloChange(eloChange);
        history.setEloAfter(eloAfter);
        saveEloHistoryPort.saveEloHistory(history);
    }
}