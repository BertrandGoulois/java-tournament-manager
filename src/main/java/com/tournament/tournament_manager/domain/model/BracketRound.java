package com.tournament.tournament_manager.domain.model;

import java.util.List;

/**
 * Un round du bracket et ses matchs (triés par position — voir {@code Match.position}).
 */
public record BracketRound(int round, List<Match> matches) {}
