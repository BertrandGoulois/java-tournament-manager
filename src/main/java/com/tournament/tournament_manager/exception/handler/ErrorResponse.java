package com.tournament.tournament_manager.exception.handler;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Corps de réponse standard pour toutes les erreurs de l'API.
 *
 * @param status    le code HTTP (ex. 404, 400, 409)
 * @param error     le libellé HTTP (ex. "Not Found", "Bad Request")
 * @param message   le message détaillé de l'erreur
 * @param timestamp l'horodatage de l'erreur
 */
public record ErrorResponse(
        @Schema(example = "404") int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}