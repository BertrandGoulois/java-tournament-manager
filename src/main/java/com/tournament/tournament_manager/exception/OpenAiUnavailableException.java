package com.tournament.tournament_manager.exception;

/**
 * Levée lorsque le service de génération de commentaire OpenAI est indisponible,
 * que ce soit suite à une erreur directe de l'API ou parce que le circuit breaker
 * est ouvert (échecs répétés détectés).
 */
public class OpenAiUnavailableException extends RuntimeException {

    public OpenAiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}