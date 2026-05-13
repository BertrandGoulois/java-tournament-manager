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

/**
 * Gère la génération et la progression du bracket en élimination directe.
 *
 * <p>Le numéro de round suit une convention décroissante : la valeur de départ est
 * la première puissance de 2 supérieure ou égale au nombre de joueurs inscrits
 * (ex. 8 pour 6 joueurs), et chaque tour suivant divise ce numéro par 2.
 * Le tournoi se termine quand {@code nextRound < 2}.
 *
 * <p>Les joueurs sans adversaire (byes) reçoivent un match {@code FINISHED}
 * immédiatement, avec eux-mêmes déclarés vainqueurs, afin d'homogénéiser
 * le traitement dans {@link #advanceToNextRound}.
 */
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

    /**
     * Démarre le tournoi et génère le bracket du premier tour.
     *
     * <p>Les joueurs inscrits sont mélangés aléatoirement avant la création
     * des matchs. Si le nombre de joueurs est impair, le dernier joueur
     * de la liste reçoit un bye (victoire automatique sans adversaire).
     *
     * @param tournamentId identifiant du tournoi à démarrer
     * @throws TournamentNotFoundException si le tournoi n'existe pas
     * @throws InvalidException si le tournoi n'est pas au statut {@code OPEN}
     * @throws InvalidException si moins de 2 joueurs sont inscrits
     */
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
            createMatch(tournament, player1, player2, calculateFirstRound(players.size()));
        }

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournamentRepository.save(tournament);
    }

    /**
     * Tente de faire progresser le bracket au tour suivant.
     *
     * <p>N'effectue aucune action si tous les matchs du {@code currentRound}
     * ne sont pas encore terminés. Quand le round suivant calculé est inférieur
     * à 2, le tournoi est marqué {@code FINISHED} (la finale vient d'être jouée).
     *
     * @param tournament   le tournoi concerné
     * @param currentRound le numéro du round qui vient de se terminer
     */
    @Transactional
    public void advanceToNextRound(Tournament tournament, int currentRound) {
        List<Match> currentMatches = matchRepository.findByTournamentIdAndRound(
                tournament.getId(), currentRound
        );
        boolean allFinished = currentMatches.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.FINISHED);
        if (!allFinished) return;

        int nextRound = currentRound / 2;
        if (nextRound < 2) {
            tournament.setStatus(TournamentStatus.FINISHED);
            tournamentRepository.save(tournament);
            return;
        }
        List<Player> winners = currentMatches.stream()
                .map(Match::getWinner)
                .collect(Collectors.toList());
        Collections.shuffle(winners);
        for (int i = 0; i < winners.size(); i += 2) {
            Player player1 = winners.get(i);
            Player player2 = (i + 1 < winners.size()) ? winners.get(i + 1) : null;
            createMatch(tournament, player1, player2, nextRound);
        }
    }

    /**
     * Crée et persiste un match entre deux joueurs pour un round donné.
     *
     * <p>Si {@code player2} est {@code null} (bye), le match est immédiatement
     * marqué {@code FINISHED} avec {@code player1} comme vainqueur.
     *
     * @param tournament le tournoi auquel appartient le match
     * @param player1    premier joueur
     * @param player2    second joueur, ou {@code null} en cas de bye
     * @param round      numéro du round
     */
    private void createMatch(Tournament tournament, Player player1, Player player2, int round) {
        Match match = new Match();
        match.setTournament(tournament);
        match.setRound(round);
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

    /**
     * Calcule le numéro du premier round, soit la plus petite puissance de 2
     * supérieure ou égale à {@code playerCount}.
     *
     * <p>Exemples : 4 joueurs → 4, 5 joueurs → 8, 8 joueurs → 8.
     *
     * @param playerCount nombre de joueurs participants
     * @return numéro du premier round
     */
    private int calculateFirstRound(int playerCount) {
        int round = 1;
        while (round < playerCount) {
            round *= 2;
        }
        return round;
    }
}
