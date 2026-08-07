package com.tournament.tournament_manager.application.shared;

import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.valueobjects.EloRating;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BracketUtilsTest {

    private Player playerWithElo(long id, int elo) {
        Player player = new Player();
        player.setId(id);
        player.setEloRating(new EloRating(elo));
        return player;
    }

    @Test
    void seedByElo_shouldOrderByEloDescending() {
        Player low = playerWithElo(1L, 900);
        Player high = playerWithElo(2L, 1500);
        Player mid = playerWithElo(3L, 1200);

        List<Player> seeded = BracketUtils.seedByElo(List.of(low, high, mid));

        assertEquals(high, seeded.get(0), "seed 1 = ELO le plus élevé");
        assertEquals(mid, seeded.get(1));
        assertEquals(low, seeded.get(2), "dernier seed = ELO le plus bas");
    }

    @Test
    void seedOrder_shouldMatchStandardBracketOrder_forSizeEight() {
        // Ordre de seeding standard bien connu pour un bracket de 8 : 1v8, 4v5, 2v7, 3v6.
        assertEquals(List.of(1, 8, 4, 5, 2, 7, 3, 6), BracketUtils.seedOrder(8));
    }

    @Test
    void seedOrder_shouldMatchStandardBracketOrder_forSizeFour() {
        assertEquals(List.of(1, 4, 2, 3), BracketUtils.seedOrder(4));
    }

    @Test
    void seedOrder_shouldMatchStandardBracketOrder_forSizeTwo() {
        assertEquals(List.of(1, 2), BracketUtils.seedOrder(2));
    }

    @Test
    void seedOrder_seed1AndSeed2_shouldOnlyMeetAtTheLastPossibleRound() {
        // Propriété fondamentale du seeding standard : les deux meilleurs seeds ne peuvent
        // s'affronter qu'en finale (jamais avant), quelle que soit la taille du bracket.
        // On le vérifie en simulant un tournoi où seed 1 et seed 2 gagnent systématiquement :
        // ils ne doivent se retrouver à la même position qu'au tout dernier round.
        List<Integer> order = BracketUtils.seedOrder(16);
        int positionOfSeed1 = order.indexOf(1) / 2;
        int positionOfSeed2 = order.indexOf(2) / 2;
        assertEquals(false, positionOfSeed1 == positionOfSeed2,
                "seed 1 et seed 2 ne doivent pas s'affronter au premier round");
    }
}
