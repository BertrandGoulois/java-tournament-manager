package com.tournament.tournament_manager.service.tournament;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.in.tournament.GetStandingsUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchesByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.dto.response.tournament.StandingEntryResponse;
import com.tournament.tournament_manager.dto.response.tournament.StandingsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cas d'utilisation : calcul du classement d'un tournoi round-robin.
 *
 * <p>Le classement est calculé à la demande à partir des matchs terminés,
 * sans table dédiée : 3 points par victoire, 0 par défaite (pas de match nul
 * dans le modèle actuel, chaque match a toujours un vainqueur). Les joueurs
 * sont triés par points décroissants, puis par nombre de victoires en cas
 * d'égalité.
 */
@Service
@Transactional(readOnly = true)
public class GetStandingsService implements GetStandingsUseCase {

    private static final int POINTS_PER_WIN = 3;

    private final LoadTournamentPort loadTournamentPort;
    private final LoadMatchesByTournamentPort loadMatchesByTournamentPort;

    public GetStandingsService(LoadTournamentPort loadTournamentPort,
                               LoadMatchesByTournamentPort loadMatchesByTournamentPort) {
        this.loadTournamentPort = loadTournamentPort;
        this.loadMatchesByTournamentPort = loadMatchesByTournamentPort;
    }

    @Override
    public StandingsResponse getStandings(Long tournamentId) {
        Tournament tournament = loadTournamentPort.loadTournament(tournamentId);
        List<Match> matches = loadMatchesByTournamentPort.loadByTournamentId(tournamentId);

        Map<Long, PlayerStats> statsByPlayer = new LinkedHashMap<>();

        for (Match match : matches) {
            registerParticipant(statsByPlayer, match.getPlayer1());
            registerParticipant(statsByPlayer, match.getPlayer2());

            if (match.getStatus() != MatchStatus.FINISHED || match.getWinner() == null) {
                continue;
            }

            Player winner = match.getWinner();
            Player loser = match.getPlayer1().equals(winner) ? match.getPlayer2() : match.getPlayer1();

            statsByPlayer.get(winner.getId()).wins++;
            if (loser != null) {
                statsByPlayer.get(loser.getId()).losses++;
            }
        }

        List<StandingEntryResponse> standings = statsByPlayer.values().stream()
                .map(this::toEntry)
                .sorted(Comparator
                        .comparingInt(StandingEntryResponse::points).reversed()
                        .thenComparing(Comparator.comparingInt(StandingEntryResponse::wins).reversed()))
                .collect(Collectors.toList());

        return new StandingsResponse(tournament.getId(), tournament.getName(), standings);
    }

    private void registerParticipant(Map<Long, PlayerStats> statsByPlayer, Player player) {
        if (player == null) {
            return; // bye
        }
        statsByPlayer.computeIfAbsent(player.getId(), id -> new PlayerStats(player));
    }

    private StandingEntryResponse toEntry(PlayerStats stats) {
        int matchesPlayed = stats.wins + stats.losses;
        return new StandingEntryResponse(
                stats.player.getId(),
                stats.player.getUsername(),
                matchesPlayed,
                stats.wins,
                stats.losses,
                stats.wins * POINTS_PER_WIN
        );
    }

    /**
     * Accumulateur interne de victoires/défaites pour un joueur,
     * utilisé uniquement pendant le calcul du classement.
     */
    private static class PlayerStats {
        private final Player player;
        private int wins;
        private int losses;

        private PlayerStats(Player player) {
            this.player = player;
        }
    }
}