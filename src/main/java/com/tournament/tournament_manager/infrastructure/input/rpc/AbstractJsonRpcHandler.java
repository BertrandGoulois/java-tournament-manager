package com.tournament.tournament_manager.infrastructure.input.rpc;

import tools.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.domain.port.out.rpc.JsonRpcMethodHandler;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Classe de base pour les handlers JSON-RPC, fournissant un utilitaire de conversion
 * des paramètres bruts ({@code Object}) vers un DTO typé via Jackson, puis validation
 * via Bean Validation.
 *
 * <p>L'{@link ObjectMapper} et le {@link Validator} sont injectés par Spring comme
 * beans partagés, évitant la création de multiples instances coûteuses.
 */
public abstract class AbstractJsonRpcHandler implements JsonRpcMethodHandler {

    protected final ObjectMapper objectMapper;
    private final Validator validator;

    protected AbstractJsonRpcHandler(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Convertit les paramètres bruts de la requête JSON-RPC vers le type attendu,
     * puis applique les contraintes {@code jakarta.validation} déclarées sur ce type.
     *
     * @throws IllegalArgumentException si une contrainte de validation est violée
     *         (traduit en erreur JSON-RPC {@code INVALID_PARAMS} par {@code JsonRpcDispatchService})
     */
    protected <T> T convertParams(Object params, Class<T> targetClass) {
        T dto = objectMapper.convertValue(params, targetClass);

        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException(message);
        }

        return dto;
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