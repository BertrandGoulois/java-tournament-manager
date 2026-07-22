package com.tournament.tournament_manager.application.bracket;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.in.tournament.AdvanceBracketUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.application.shared.BracketUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cas d'utilisation : avancement du bracket au tour suivant.
 */
@Service
@Transactional
public class AdvanceBracketService implements AdvanceBracketUseCase {

    private final SaveTournamentPort saveTournamentPort;
    private final LoadMatchByTournamentPort loadMatchByTournamentPort;
    private final SaveMatchPort saveMatchPort;

    public AdvanceBracketService(SaveTournamentPort saveTournamentPort,
                                 LoadMatchByTournamentPort loadMatchByTournamentPort,
                                 SaveMatchPort saveMatchPort) {
        this.saveTournamentPort = saveTournamentPort;
        this.loadMatchByTournamentPort = loadMatchByTournamentPort;
        this.saveMatchPort = saveMatchPort;
    }

    @Override
    public void advanceToNextRound(Tournament tournament, int currentRound) {
        List<Match> currentMatches = loadMatchByTournamentPort
                .loadByTournamentIdAndRound(tournament.getId(), currentRound);
        boolean allFinished = currentMatches.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.FINISHED);
        if (!allFinished) return;

        int nextRound = currentRound / 2;
        if (nextRound < 2) {
            tournament.setStatus(TournamentStatus.FINISHED);
            saveTournamentPort.saveTournament(tournament);
            return;
        }
        List<Player> winners = currentMatches.stream()
                .map(Match::getWinner)
                .collect(Collectors.toList());
        Collections.shuffle(winners);
        for (int i = 0; i < winners.size(); i += 2) {
            Player player1 = winners.get(i);
            Player player2 = (i + 1 < winners.size()) ? winners.get(i + 1) : null;
            BracketUtils.createMatch(tournament, player1, player2, nextRound, saveMatchPort);
        }
    }
}