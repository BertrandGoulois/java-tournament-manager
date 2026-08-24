package com.tournament.tournament_manager.dto.response.rpc;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Erreur conforme à la spécification JSON-RPC 2.0.
 *
 * @param code    code d'erreur ({@code -32700} parse error, {@code -32600} invalid request,
 *                {@code -32601} method not found, {@code -32602} invalid params,
 *                {@code -32603} internal error - codes standard réservés par la spec ;
 *                {@code -32000} à {@code -32002} - codes serveur définis par cette API,
 *                dans la plage {@code -32000}/{@code -32099} que la spec réserve à cet usage)
 * @param message message d'erreur lisible
 * @param data    détail optionnel (ex. message de l'exception métier)
 */
public record JsonRpcError(
        @Schema(example = "-32601") int code,
        String message,
        Object data
) {
    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    /**
     * Erreur métier "attendue" (ressource introuvable, déjà existante, règle métier violée).
     * Distincte de {@link #INTERNAL_ERROR} : une erreur métier est un rejet normal du serveur
     * face à une requête qu'il comprend parfaitement, pas un signe de dysfonctionnement -
     * les deux étaient auparavant indistinguables sous le même code, ce qui empêchait toute
     * supervision fiable basée sur les taux d'erreur (voir {@code JsonRpcController}).
     */
    public static final int BUSINESS_ERROR = -32000;
    /** L'utilisateur authentifié n'a pas le rôle requis pour cette méthode. */
    public static final int ACCESS_DENIED = -32001;
    /** Conflit de modification concurrente (verrouillage optimiste). */
    public static final int CONFLICT = -32002;
}