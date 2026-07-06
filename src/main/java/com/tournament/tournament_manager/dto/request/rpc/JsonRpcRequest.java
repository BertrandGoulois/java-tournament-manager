package com.tournament.tournament_manager.dto.request.rpc;

/**
 * Enveloppe de requête conforme à la spécification JSON-RPC 2.0.
 *
 * @param jsonrpc version du protocole, doit être {@code "2.0"}
 * @param method  nom de la méthode à invoquer (ex. {@code "tournament.create"})
 * @param params  paramètres de la méthode, désérialisés selon le handler concerné
 * @param id      identifiant de corrélation fourni par le client (chaîne, nombre, ou null pour une notification)
 */
public record JsonRpcRequest(
        String jsonrpc,
        String method,
        Object params,
        Object id
) {
}