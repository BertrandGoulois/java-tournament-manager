package com.tournament.tournament_manager.service;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Registration;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.exception.InvalidException;
import com.tournament.tournament_manager.exception.TournamentNotFoundException;
import com.tournament.tournament_manager.repository.MatchRepository;
import com.tournament.tournament_manager.repository.RegistrationRepository;
import com.tournament.tournament_manager.repository.TournamentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class BracketService {

    private final TournamentRepository tournamentRepository;
    private final RegistrationRepository registrationRepository;
    private final MatchRepository matchRepository;

    public BracketService(TournamentRepository tournamentRepository,
                          RegistrationRepository registrationRepository,
                          MatchRepository matchRepository) {
        this.tournamentRepository = tournamentRepository;
        this.registrationRepository = registrationRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional
    public void startTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new TournamentNotFoundException(tournamentId));
        if (tournament.getStatus() != TournamentStatus.OPEN) {
            throw new InvalidException("Tournament is not open");
        }
        List<Registration> registrations = registrationRepository.findByTournamentId(tournamentId);
        if (registrations.size() < 2) {
            throw new InvalidException("Tournament needs at least 2 players");
        }
        List<Player> players = registrations.stream()
                .map(Registration::getPlayer)
                .collect(Collectors.toList());
        Collections.shuffle(players);

        for (int i = 0; i < players.size(); i += 2) {
            Player player1 = players.get(i);
            Player player2 = (i + 1 < players.size()) ? players.get(i + 1) : null;
            Match match = new Match();
            match.setTournament(tournament);
            match.setRound(calculateFirstRound(players.size()));
            match.setPlayer1(player1);
            match.setPlayer2(player2);

            if (player2 == null) {
                match.setStatus(MatchStatus.FINISHED);
                match.setWinner(player1);
                match.setPlayedAt(LocalDateTime.now());
            } else {
                match.setStatus(MatchStatus.PENDING);
            }

            matchRepository.save(match);
        }

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournamentRepository.save(tournament);
    }

    private int calculateFirstRound(int playerCount) {
        int round = 1;
        while (round < playerCount) {
            round *= 2;
        }
        return round;
    }
}
