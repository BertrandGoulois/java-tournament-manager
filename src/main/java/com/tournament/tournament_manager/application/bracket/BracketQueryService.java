package com.tournament.tournament_manager.application.bracket;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.port.in.tournament.GetBracketUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchesByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import com.tournament.tournament_manager.dto.response.tournament.BracketMatchResponse;
import com.tournament.tournament_manager.dto.response.tournament.BracketResponse;
import com.tournament.tournament_manager.dto.response.tournament.BracketRoundResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implémentation du cas d'utilisation de consultation du bracket.
 *
 * <p>Charge tous les matchs du tournoi et les regroupe par round,
 * triés du premier round (valeur la plus haute) à la finale (round 2).
 */
@Service
@Transactional(readOnly = true)
public class BracketQueryService implements GetBracketUseCase {

    private final LoadTournamentPort loadTournamentPort;
    private final LoadMatchesByTournamentPort loadMatchesByTournamentPort;

    public BracketQueryService(LoadTournamentPort loadTournamentPort,
                               LoadMatchesByTournamentPort loadMatchesByTournamentPort) {
        this.loadTournamentPort = loadTournamentPort;
        this.loadMatchesByTournamentPort = loadMatchesByTournamentPort;
    }

    /**
     * Retourne le bracket complet d'un tournoi, organisé par round.
     *
     * @param tournamentId identifiant du tournoi
     * @return le bracket avec tous les matchs groupés par round
     */
    @Override
    public BracketResponse getBracket(Long tournamentId) {
        Tournament tournament = loadTournamentPort.loadTournament(tournamentId);
        List<Match> matches = loadMatchesByTournamentPort.loadByTournamentId(tournamentId);

        Map<Integer, List<Match>> matchesByRound = matches.stream()
                .collect(Collectors.groupingBy(Match::getRound));

        List<BracketRoundResponse> rounds = matchesByRound.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<Match>>comparingByKey().reversed())
                .map(entry -> new BracketRoundResponse(
                        entry.getKey(),
                        entry.getValue().stream()
                                .map(this::toMatchResponse)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return new BracketResponse(
                tournament.getId(),
                tournament.getName(),
                tournament.getStatus(),
                rounds
        );
    }

    private BracketMatchResponse toMatchResponse(Match match) {
        return new BracketMatchResponse(
                match.getId(),
                match.getPlayer1().getId(),
                match.getPlayer2() != null ? match.getPlayer2().getId() : null,
                match.getWinner() != null ? match.getWinner().getId() : null,
                match.getStatus()
        );
    }
}