package com.tournament.tournament_manager.application.bracket;

import com.tournament.tournament_manager.domain.model.Bracket;
import com.tournament.tournament_manager.domain.model.BracketRound;
import com.tournament.tournament_manager.domain.model.Match;
import com.tournament.tournament_manager.domain.model.Tournament;
import com.tournament.tournament_manager.domain.port.in.tournament.GetBracketUseCase;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchesByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.tournament.LoadTournamentPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implémentation du cas d'utilisation de consultation du bracket.
 *
 * <p>Charge tous les matchs du tournoi et les regroupe par round,
 * triés du premier round (valeur la plus haute) à la finale (round 2).
 * Retourne un {@link Bracket} pur, réutilisant directement les objets domaine
 * {@link Match} pour chaque match — voir la Javadoc de {@link Bracket}.
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
    public Bracket getBracket(Long tournamentId) {
        Tournament tournament = loadTournamentPort.loadTournament(tournamentId);
        List<Match> matches = loadMatchesByTournamentPort.loadByTournamentId(tournamentId);

        Map<Integer, List<Match>> matchesByRound = matches.stream()
                .collect(Collectors.groupingBy(Match::getRound));

        List<BracketRound> rounds = matchesByRound.entrySet().stream()
                .sorted(Map.Entry.<Integer, List<Match>>comparingByKey().reversed())
                .map(entry -> new BracketRound(
                        entry.getKey(),
                        entry.getValue().stream()
                                .sorted(Comparator.comparingInt(Match::getPosition))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        return new Bracket(
                tournament.getId(),
                tournament.getName().value(),
                tournament.getStatus(),
                rounds
        );
    }
}
