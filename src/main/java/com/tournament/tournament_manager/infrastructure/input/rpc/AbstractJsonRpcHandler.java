package com.tournament.tournament_manager.infrastructure.input.rpc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.domain.port.out.rpc.JsonRpcMethodHandler;

/**
 * Classe de base pour les handlers JSON-RPC, fournissant un utilitaire de conversion
 * des paramètres bruts ({@code Object}) vers un DTO typé via Jackson.
 *
 * <p>L'{@link ObjectMapper} est injecté par Spring comme bean partagé,
 * évitant la création de multiples instances coûteuses.
 */
public abstract class AbstractJsonRpcHandler implements JsonRpcMethodHandler {

    protected final ObjectMapper objectMapper;

    protected AbstractJsonRpcHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Convertit les paramètres bruts de la requête JSON-RPC vers le type attendu.
     */
    protected <T> T convertParams(Object params, Class<T> targetClass) {
        return objectMapper.convertValue(params, targetClass);
    }

    /**
     * Extrait un paramètre de type {@code Long} depuis les paramètres bruts.
     *
     * @param params les paramètres bruts de la requête JSON-RPC
     * @param key    le nom du paramètre à extraire
     * @return la valeur du paramètre sous forme de {@code Long}
     * @throws ClassCastException si la valeur n'est pas un nombre
     * @throws NullPointerException si la clé est absente
     */
    protected Long getLong(Object params, String key) {
        java.util.Map<?, ?> map = objectMapper.convertValue(params, java.util.Map.class);
        return ((Number) map.get(key)).longValue();
    }
}