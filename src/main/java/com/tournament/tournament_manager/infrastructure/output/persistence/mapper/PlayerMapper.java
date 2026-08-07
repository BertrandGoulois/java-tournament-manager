package com.tournament.tournament_manager.infrastructure.output.persistence.mapper;

import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.valueobjects.EloRating;
import com.tournament.tournament_manager.infrastructure.output.persistence.entity.PlayerEntity;
import org.springframework.stereotype.Component;

/**
 * Convertit entre le domaine pur {@link Player} et sa contrepartie JPA {@link PlayerEntity}.
 *
 * <p>{@link #updateEntity} est la méthode à utiliser pour toute mise à jour d'un joueur déjà
 * persisté : elle copie les champs sur une entité <b>déjà chargée</b> (donc avec son
 * {@code @Version} intact, géré par Hibernate), plutôt que de construire une entité neuve à
 * l'aveugle — c'est ce qui permet au verrouillage optimiste (point 20) de continuer à
 * fonctionner correctement après la séparation domaine/persistance.
 */
@Component
public class PlayerMapper {

    public Player toDomain(PlayerEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Player(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                new EloRating(entity.getEloRating()),
                entity.getCreatedAt(),
                entity.isDeleted(),
                entity.getDeletedAt(),
                entity.getAnonymizedAt()
        );
    }

    /**
     * Construit une nouvelle entité pour un joueur pas encore persisté (pas d'id).
     */
    public PlayerEntity toNewEntity(Player player) {
        PlayerEntity entity = new PlayerEntity();
        updateEntity(entity, player);
        return entity;
    }

    /**
     * Copie les champs de {@code player} sur une entité déjà chargée (voir Javadoc de
     * la classe). N'écrit jamais {@code id} ni {@code version} : gérés par Hibernate.
     */
    public void updateEntity(PlayerEntity entity, Player player) {
        entity.setUsername(player.getUsername());
        entity.setEmail(player.getEmail());
        entity.setEloRating(player.getEloRating().value());
        entity.setDeleted(player.isDeleted());
        entity.setDeletedAt(player.getDeletedAt());
        entity.setAnonymizedAt(player.getAnonymizedAt());
        if (player.getCreatedAt() != null) {
            entity.setCreatedAt(player.getCreatedAt());
        }
    }
}
