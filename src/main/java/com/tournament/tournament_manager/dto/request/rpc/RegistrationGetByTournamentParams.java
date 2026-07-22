package com.tournament.tournament_manager.dto.request.rpc;

public record  RegistrationGetByTournamentParams (
        Long tournamentId,
        int page,
        int size) {
}
