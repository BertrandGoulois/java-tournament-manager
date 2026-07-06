package com.tournament.tournament_manager.domain.port.out.rpc;

/**
 * Handler d'une méthode JSON-RPC exposée par l'API.
 *
 * <p>Chaque implémentation correspond à une opération métier (ex. {@code tournament.create}),
 * et délègue au use case correspondant après avoir désérialisé les paramètres.
 */
public interface JsonRpcMethodHandler {

    /**
     * Nom de la méthode JSON-RPC gérée par ce handler (ex. {@code "tournament.create"}).
     *
     * @return le nom de la méthode
     */
    String methodName();

    /**
     * Exécute la méthode à partir des paramètres bruts de la requête.
     *
     * @param params les paramètres JSON de la requête, à désérialiser selon le besoin du handler
     * @return le résultat à encapsuler dans la réponse JSON-RPC
     */
    Object handle(Object params);
}