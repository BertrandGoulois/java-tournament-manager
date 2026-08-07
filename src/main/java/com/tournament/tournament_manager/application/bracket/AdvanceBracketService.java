package com.tournament.tournament_manager.application.bracket;

import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.in.tournament.AdvanceBracketUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import com.tournament.tournament_manager.domain.port.out.tournament.ClaimRoundAdvancementPort;
import com.tournament.tournament_manager.domain.port.out.tournament.SaveTournamentPort;
import com.tournament.tournament_manager.application.shared.BracketUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cas d'utilisation : avancement du bracket au tour suivant.
 *
 * <p>Idempotent face à la redelivery Kafka (at-least-once) : avant de créer les matchs du
 * round suivant, {@link ClaimRoundAdvancementPort#tryClaim} réserve ce round de façon
 * atomique (contrainte d'unicité en base). Si le round a déjà été réclamé — par cette même
 * méthode appelée deux fois pour le même événement redistribué, ou par un appel concurrent
 * sur un autre thread/partition — la création est silencieusement ignorée plutôt que de
 * dupliquer le round avec un tirage au sort différent.
 *
 * <p>L'avancement est déterministe, pas tiré au hasard : le vainqueur des matchs aux
 * positions {@code 2k} et {@code 2k+1} du round courant avance vers la position {@code k}
 * du round suivant — c'est ce qui fait du bracket un vrai arbre plutôt qu'un simple
 * groupement de matchs (voir {@code BracketUtils.seedOrder} pour le seeding initial).
 */
@Slf4j
@Service
@Transactional
public class AdvanceBracketService implements AdvanceBracketUseCase {

    private final SaveTournamentPort saveTournamentPort;
    private final LoadMatchByTournamentPort loadMatchByTournamentPort;
    private final SaveMatchPort saveMatchPort;
    private final ClaimRoundAdvancementPort claimRoundAdvancementPort;

    public AdvanceBracketService(SaveTournamentPort saveTournamentPort,
                                 LoadMatchByTournamentPort loadMatchByTournamentPort,
                                 SaveMatchPort saveMatchPort,
                                 ClaimRoundAdvancementPort claimRoundAdvancementPort) {
        this.saveTournamentPort = saveTournamentPort;
        this.loadMatchByTournamentPort = loadMatchByTournamentPort;
        this.saveMatchPort = saveMatchPort;
        this.claimRoundAdvancementPort = claimRoundAdvancementPort;
    }

    @Override
    public void advanceToNextRound(Tournament tournament, int currentRound) {
        List<Match> currentMatches = loadMatchByTournamentPort
                .loadByTournamentIdAndRound(tournament.getId(), currentRound);
        boolean allFinished = currentMatches.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.FINISHED);
        if (!allFinished) return;

        int nextRound = currentRound / 2;

        if (!claimRoundAdvancementPort.tryClaim(tournament.getId(), nextRound)) {
            log.warn("Round déjà créé ou en cours de création, avancement ignoré "
                    + "[tournamentId={}, nextRound={}]", tournament.getId(), nextRound);
            return;
        }

        if (nextRound < 2) {
            tournament.setStatus(TournamentStatus.FINISHED);
            saveTournamentPort.saveTournament(tournament);
            return;
        }

        // Trié par position : l'index dans cette liste EST la position au sein du round
        // courant, ce qui permet un appariement déterministe pour le round suivant.
        List<Player> winnersByPosition = currentMatches.stream()
                .sorted(Comparator.comparingInt(Match::getPosition))
                .map(Match::getWinner)
                .collect(Collectors.toList());

        for (int i = 0; i < winnersByPosition.size(); i += 2) {
            Player player1 = winnersByPosition.get(i);
            Player player2 = (i + 1 < winnersByPosition.size()) ? winnersByPosition.get(i + 1) : null;
            int nextPosition = i / 2;
            BracketUtils.createMatch(tournament, player1, player2, nextRound, nextPosition, saveMatchPort);
        }
    }
}