package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.in.tournament.CheckTournamentCompletionUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchesByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cas d'utilisation : détection de fin d'un tournoi round-robin.
 *
 * <p>Contrairement à l'élimination directe (qui avance round par round),
 * le round-robin n'a pas de notion de "tour suivant" : tous les matchs sont
 * générés d'un coup au démarrage, et le tournoi est terminé dès que chacun
 * d'entre eux a un statut {@code FINISHED}.
 */
@Service
@Transactional
public class CheckTournamentCompletionService implements CheckTournamentCompletionUseCase {

    private final LoadMatchesByTournamentPort loadMatchesByTournamentPort;
    private final SaveTournamentPort saveTournamentPort;

    public CheckTournamentCompletionService(LoadMatchesByTournamentPort loadMatchesByTournamentPort,
                                            SaveTournamentPort saveTournamentPort) {
        this.loadMatchesByTournamentPort = loadMatchesByTournamentPort;
        this.saveTournamentPort = saveTournamentPort;
    }

    @Override
    public void checkCompletion(Tournament tournament) {
        List<Match> matches = loadMatchesByTournamentPort.loadByTournamentId(tournament.getId());

        boolean allFinished = !matches.isEmpty() && matches.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.FINISHED);

        if (allFinished) {
            tournament.setStatus(TournamentStatus.FINISHED);
            saveTournamentPort.saveTournament(tournament);
        }
    }
}