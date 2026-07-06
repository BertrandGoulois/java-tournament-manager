package com.tournament.tournament_manager.dto.response.rpc;

/**
 * Erreur conforme à la spécification JSON-RPC 2.0.
 *
 * @param code    code d'erreur standard ({@code -32700} parse error, {@code -32601} method not found,
 *                {@code -32602} invalid params, {@code -32603} internal error)
 * @param message message d'erreur lisible
 * @param data    détail optionnel (ex. message de l'exception métier)
 */
public record JsonRpcError(
        int code,
        String message,
        Object data
) {
    public static final int PARSE_ERROR = -32700;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;
}