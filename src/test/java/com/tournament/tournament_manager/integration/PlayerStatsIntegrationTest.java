package com.tournament.tournament_manager.integration;

import com.tournament.tournament_manager.TestcontainersConfiguration;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.MatchEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.TournamentEntity;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.out.player.CountMatchesByPlayerPort;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.MatchRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.PlayerRepository;
import com.tournament.tournament_manager.infrastructure.output.persistence.repository.TournamentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reproduit exactement le scénario décrit dans la revue : un joueur avec un bye et des
 * matchs à venir ne doit afficher AUCUN match joué tant qu'il n'a pas réellement joué —
 * avant le correctif, il affichait 3 matchs joués / 1 victoire / 2 défaites / 33% de
 * winrate rien qu'en ayant été placé dans le bracket.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PlayerStatsIntegrationTest {

    @Autowired
    private CountMatchesByPlayerPort countMatchesByPlayerPort;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private MatchRepository matchRepository;

    private TournamentEntity newTournament() {
        TournamentEntity tournament = new TournamentEntity();
        tournament.setName("tournoi-stats-test-" + System.nanoTime());
        tournament.setMaxPlayers(8);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournament.setFormat(TournamentFormat.SINGLE_ELIMINATION);
        return tournamentRepository.save(tournament);
    }

    private PlayerEntity newPlayer(String prefix) {
        PlayerEntity player = new PlayerEntity();
        player.setUsername(prefix + "_" + System.nanoTime());
        player.setEmail(prefix + "_" + System.nanoTime() + "@mail.com");
        return playerRepository.save(player);
    }

    @Test
    void countByPlayer_shouldBeZero_forPlayerWithOnlyAByeAndPendingMatches() {
        TournamentEntity tournament = newTournament();
        PlayerEntity player = newPlayer("bye_player");
        PlayerEntity opponent = newPlayer("future_opponent");

        // Le bye du premier tour : status FINISHED, mais un seul joueur réel (player2 null).
        MatchEntity bye = new MatchEntity();
        bye.setTournament(tournament);
        bye.setRound(8);
        bye.setPosition(0);
        bye.setPlayer1(player);
        bye.setPlayer2(null);
        bye.setWinner(player);
        bye.setStatus(MatchStatus.FINISHED);
        bye.setPlayedAt(Instant.now());
        matchRepository.save(bye);

        // Un match à venir au second tour : PENDING, deux joueurs réels, mais pas encore joué.
        MatchEntity pending = new MatchEntity();
        pending.setTournament(tournament);
        pending.setRound(4);
        pending.setPosition(0);
        pending.setPlayer1(player);
        pending.setPlayer2(opponent);
        pending.setStatus(MatchStatus.PENDING);
        matchRepository.save(pending);

        // Avant le correctif, le bye ET le match PENDING étaient tous les deux comptés
        // comme des matchs "joués" — c'est exactement le bug décrit dans la revue.
        assertEquals(0, countMatchesByPlayerPort.countByPlayer(player.getId()),
                "Ni le bye ni le match PENDING ne doivent compter comme un match joué");
        assertEquals(0, countMatchesByPlayerPort.countWinsByPlayer(player.getId()),
                "Le bye ne doit pas compter comme une victoire");
    }

    @Test
    void countByPlayer_shouldCountOnlyRealFinishedMatches() {
        TournamentEntity tournament = newTournament();
        PlayerEntity player = newPlayer("real_player");
        PlayerEntity winner1 = newPlayer("opponent1");
        PlayerEntity opponent2 = newPlayer("opponent2");

        // 1 vraie victoire
        MatchEntity won = new MatchEntity();
        won.setTournament(tournament);
        won.setRound(8);
        won.setPosition(0);
        won.setPlayer1(player);
        won.setPlayer2(winner1);
        won.setWinner(player);
        won.setStatus(MatchStatus.FINISHED);
        won.setPlayedAt(Instant.now());
        matchRepository.save(won);

        // 1 vraie défaite
        MatchEntity lost = new MatchEntity();
        lost.setTournament(tournament);
        lost.setRound(4);
        lost.setPosition(0);
        lost.setPlayer1(player);
        lost.setPlayer2(opponent2);
        lost.setWinner(opponent2);
        lost.setStatus(MatchStatus.FINISHED);
        lost.setPlayedAt(Instant.now());
        matchRepository.save(lost);

        // 1 bye (ne doit pas compter) + 1 match futur PENDING (ne doit pas compter)
        MatchEntity bye = new MatchEntity();
        bye.setTournament(tournament);
        bye.setRound(2);
        bye.setPosition(0);
        bye.setPlayer1(player);
        bye.setPlayer2(null);
        bye.setWinner(player);
        bye.setStatus(MatchStatus.FINISHED);
        bye.setPlayedAt(Instant.now());
        matchRepository.save(bye);

        assertEquals(2, countMatchesByPlayerPort.countByPlayer(player.getId()),
                "Seuls les 2 vrais matchs FINISHED doivent compter, pas le bye");
        assertEquals(1, countMatchesByPlayerPort.countWinsByPlayer(player.getId()),
                "1 seule vraie victoire, le bye ne doit pas être compté");
    }
}
