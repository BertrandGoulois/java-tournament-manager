package com.tournament.tournament_manager.dto.response.rpc;

/**
 * Enveloppe de réponse conforme à la spécification JSON-RPC 2.0.
 *
 * <p>Exactement un des deux champs {@code result} ou {@code error} doit être renseigné,
 * jamais les deux (garanti par les méthodes factory ci-dessous).
 *
 * @param jsonrpc version du protocole, toujours {@code "2.0"}
 * @param result  résultat de l'appel en cas de succès, {@code null} sinon
 * @param error   erreur en cas d'échec, {@code null} sinon
 * @param id      identifiant de corrélation, recopié depuis la requête
 */
public record JsonRpcResponse(
        String jsonrpc,
        Object result,
        JsonRpcError error,
        Object id
) {
    public static JsonRpcResponse success(Object result, Object id) {
        return new JsonRpcResponse("2.0", result, null, id);
    }

    public static JsonRpcResponse failure(JsonRpcError error, Object id) {
        return new JsonRpcResponse("2.0", null, error, id);
    }
}