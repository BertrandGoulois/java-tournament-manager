package com.tournament.tournament_manager.domain.model;

import java.util.List;
import java.util.function.Function;

/**
 * Page de résultats, pure — équivalent domaine de {@code org.springframework.data.domain.Page}.
 *
 * <p>Conversion aux frontières : les adaptateurs JPA convertissent le {@code Page<Entity>}
 * retourné par Spring Data en {@code PageResult<Domain>} ; les contrôleurs REST reconvertissent
 * en {@code org.springframework.data.domain.PageImpl} pour préserver exactement le même
 * contrat JSON qu'avant (métadonnées de pagination Spring standard) — ce n'est pas au domaine
 * de porter cette préoccupation de sérialisation HTTP.
 */
public record PageResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResult<>(content, page, size, totalElements, totalPages);
    }

    public <R> PageResult<R> map(Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
