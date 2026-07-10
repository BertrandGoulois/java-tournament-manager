package com.tournament.tournament_manager.application.tournament;

import com.tournament.tournament_manager.domain.model.entities.Match;
import com.tournament.tournament_manager.domain.model.entities.Player;
import com.tournament.tournament_manager.domain.model.entities.Tournament;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.out.match.LoadMatchesByTournamentPort;
import com.tournament.tournament_manager.domain.port.out.match.SaveMatchPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenerateKnockoutBracketFromGroupsServiceTest {

    @Mock
    private LoadMatchesByTournamentPort loadMatchesByTournamentPort;
    @Mock
    private SaveMatchPort saveMatchPort;

    @InjectMocks
    private GenerateKnockoutBracketFromGroupsService service;

    @Test
    void shouldNotGenerateBracket_whenSomeGroupMatchesStillPending() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setQualifiersPerGroup(1);

        Player p1 = new Player();
        p1.setId(1L);
        Player p2 = new Player();
        p2.setId(2L);

        Match finished = groupMatch(p1, p2, p1, 1, MatchStatus.FINISHED);
        Match pending = groupMatch(p1, p2, null, 2, MatchStatus.PENDING);

        when(loadMatchesByTournamentPort.loadByTournamentId(1L)).thenReturn(List.of(finished, pending));

        service.checkGroupsCompletionAndGenerateBracket(tournament);

        verify(saveMatchPort, never()).saveMatch(any());
    }

    @Test
    void shouldGenerateBracket_whenAllGroupMatchesFinished() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setQualifiersPerGroup(1);

        // Groupe 1 : alice bat bob -> alice qualifiée
        Player alice = new Player();
        alice.setId(1L);
        Player bob = new Player();
        bob.setId(2L);
        Match group1Match = groupMatch(alice, bob, alice, 1, MatchStatus.FINISHED);

        // Groupe 2 : carol bat dave -> carol qualifiée
        Player carol = new Player();
        carol.setId(3L);
        Player dave = new Player();
        dave.setId(4L);
        Match group2Match = groupMatch(carol, dave, carol, 2, MatchStatus.FINISHED);

        when(loadMatchesByTournamentPort.loadByTournamentId(1L))
                .thenReturn(List.of(group1Match, group2Match));

        service.checkGroupsCompletionAndGenerateBracket(tournament);

        // 2 qualifiés (alice, carol) -> 1 match de bracket
        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(1)).saveMatch(captor.capture());

        Match bracketMatch = captor.getValue();
        assertNull(bracketMatch.getGroupNumber());
        Set<Long> participantIds = new HashSet<>();
        participantIds.add(bracketMatch.getPlayer1().getId());
        if (bracketMatch.getPlayer2() != null) {
            participantIds.add(bracketMatch.getPlayer2().getId());
        }
        assertEquals(Set.of(1L, 3L), participantIds);
    }

    @Test
    void shouldNotRegenerateBracket_whenAlreadyGenerated() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setQualifiersPerGroup(1);

        Player alice = new Player();
        alice.setId(1L);
        Player bob = new Player();
        bob.setId(2L);
        Match group1Match = groupMatch(alice, bob, alice, 1, MatchStatus.FINISHED);

        Match bracketMatch = new Match();
        bracketMatch.setGroupNumber(null);
        bracketMatch.setPlayer1(alice);
        bracketMatch.setStatus(MatchStatus.PENDING);

        when(loadMatchesByTournamentPort.loadByTournamentId(1L))
                .thenReturn(List.of(group1Match, bracketMatch));

        service.checkGroupsCompletionAndGenerateBracket(tournament);

        verify(saveMatchPort, never()).saveMatch(any());
    }

    @Test
    void shouldSelectTopNQualifiersPerGroup_whenQualifiersPerGroupGreaterThanOne() {
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        tournament.setQualifiersPerGroup(2);

        Player alice = new Player();
        alice.setId(1L);
        Player bob = new Player();
        bob.setId(2L);
        Player carol = new Player();
        carol.setId(3L);

        // Round-robin à 3 dans le groupe 1 : alice bat bob, alice bat carol, bob bat carol
        // Classement : alice (6 pts), bob (3 pts), carol (0 pts) -> top 2 = alice, bob
        Match m1 = groupMatch(alice, bob, alice, 1, MatchStatus.FINISHED);
        Match m2 = groupMatch(alice, carol, alice, 1, MatchStatus.FINISHED);
        Match m3 = groupMatch(bob, carol, bob, 1, MatchStatus.FINISHED);

        when(loadMatchesByTournamentPort.loadByTournamentId(1L))
                .thenReturn(List.of(m1, m2, m3));

        service.checkGroupsCompletionAndGenerateBracket(tournament);

        ArgumentCaptor<Match> captor = ArgumentCaptor.forClass(Match.class);
        verify(saveMatchPort, times(1)).saveMatch(captor.capture());

        Match bracketMatch = captor.getValue();
        Set<Long> participantIds = new HashSet<>();
        participantIds.add(bracketMatch.getPlayer1().getId());
        if (bracketMatch.getPlayer2() != null) {
            participantIds.add(bracketMatch.getPlayer2().getId());
        }
        assertEquals(Set.of(1L, 2L), participantIds); // alice et bob, pas carol
    }

    private Match groupMatch(Player player1, Player player2, Player winner, int groupNumber, MatchStatus status) {
        Match match = new Match();
        match.setPlayer1(player1);
        match.setPlayer2(player2);
        match.setWinner(winner);
        match.setGroupNumber(groupNumber);
        match.setStatus(status);
        return match;
    }
}