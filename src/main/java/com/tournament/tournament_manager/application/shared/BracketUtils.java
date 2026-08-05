package com.tournament.tournament_manager.application.shared;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilitaires partagés pour la gestion du bracket.
 *
 * <p>Le bracket est <b>seedé</b> par classement ELO, jamais tiré au hasard : le seed 1 (ELO
 * le plus élevé) et le seed 2 ne peuvent se rencontrer qu'en finale, conformément à l'ordre
 * de seeding standard des tournois à élimination directe (voir {@link #seedOrder}).
 */
public class BracketUtils {

    private BracketUtils() {}

    public static void createMatch(Tournament tournament, Player player1, Player player2,
                                   int round, int position, SaveMatchPort saveMatchPort) {
        Match match = new Match();
        match.setTournament(tournament);
        match.setRound(round);
        match.setPosition(position);
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        if (player2 == null) {
            match.setStatus(MatchStatus.FINISHED);
            match.setWinner(player1);
            match.setPlayedAt(Instant.now());
        } else {
            match.setStatus(MatchStatus.PENDING);
        }
        saveMatchPort.saveMatch(match);
    }

    public static int calculateFirstRound(int playerCount) {
        if (playerCount <= 0) {
            throw new IllegalArgumentException("playerCount must be positive, got: " + playerCount);
        }
        // Un bracket d'un seul participant a quand même besoin d'un match (un bye) pour le
        // faire "avancer" — la taille minimale du bracket est donc toujours 2, jamais 1.
        int round = 2;
        while (round < playerCount) {
            round *= 2;
        }
        return round;
    }

    /**
     * Trie les joueurs par ELO décroissant : index 0 = seed 1 (ELO le plus élevé),
     * index 1 = seed 2, etc.
     */
    public static List<Player> seedByElo(List<Player> players) {
        return players.stream()
                .sorted(Comparator.comparingInt((Player p) -> p.getEloRating().value()).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Ordre de seeding standard d'un bracket à élimination directe de taille {@code bracketSize}
     * (puissance de 2) : la liste retournée donne, position par position, le numéro de seed
     * (1-indexé) occupant chaque slot. Les positions {@code 2k} et {@code 2k+1} s'affrontent
     * au premier round.
     *
     * <p>Exemple pour {@code bracketSize = 8} : {@code [1, 8, 4, 5, 2, 7, 3, 6]} — seed 1
     * affronte seed 8, seed 4 affronte seed 5, etc. Cet ordre garantit que les meilleurs
     * seeds ne peuvent se rencontrer qu'au tour le plus tardif possible compte tenu de leur
     * rang (seed 1 et seed 2 ne peuvent s'affronter qu'en finale, seeds 1-4 pas avant les
     * demi-finales, etc.) — c'est l'algorithme de seeding standard utilisé par la quasi
     * totalité des tournois à élimination directe.
     *
     * <p>Un numéro de seed supérieur à l'effectif réel de joueurs représente un slot vide
     * (bye) : voir son usage dans {@code SingleEliminationStartStrategy}.
     */
    public static List<Integer> seedOrder(int bracketSize) {
        List<Integer> seeds = new ArrayList<>(List.of(1));
        while (seeds.size() < bracketSize) {
            int size = seeds.size() * 2 + 1;
            List<Integer> next = new ArrayList<>(seeds.size() * 2);
            for (int s : seeds) {
                next.add(s);
                next.add(size - s);
            }
            seeds = next;
        }
        return seeds;
    }
}
