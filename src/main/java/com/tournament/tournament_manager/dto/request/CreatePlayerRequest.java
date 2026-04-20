package com.tournament.tournament_manager.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreatePlayerRequest(
        @NotBlank String username,
        @NotBlank @Email String email
) {}
