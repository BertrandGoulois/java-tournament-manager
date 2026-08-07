package com.tournament.tournament_manager.domain.model;

/**
 * Requête de pagination, pure — équivalent domaine de {@code org.springframework.data.domain.Pageable}.
 *
 * <p>Le tri (disponible sur {@code Pageable} mais jamais utilisé nulle part dans ce projet)
 * n'est volontairement pas repris ici : ajouter une capacité non exercée par aucun appelant
 * aurait été de la complexité gratuite. À réintroduire si un vrai besoin de tri apparaît.
 *
 * <p>Conversion aux frontières : les contrôleurs REST convertissent le {@code Pageable}
 * résolu par Spring MVC en {@code PageRequest}, et les adaptateurs JPA reconvertissent en
 * {@code Pageable} juste avant d'appeler {@code repository.findAll(...)}.
 */
public record PageRequest(int page, int size) {

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size);
    }
}
