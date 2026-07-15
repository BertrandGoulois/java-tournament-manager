package com.tournament.tournament_manager.dto.request.player;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePlayerRequest(
        @NotBlank(message = "Le nom d'utilisateur est obligatoire")
        @Size(min = 3, max = 30, message = "Le nom d'utilisateur doit contenir entre 3 et 30 caractères")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Le nom d'utilisateur ne peut contenir que des lettres, chiffres, tirets et underscores")
        String username,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "L'email n'est pas valide")
        @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
        String email
) {}
