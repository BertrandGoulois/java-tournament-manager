package com.tournament.tournament_manager.domain.model.enums;

/**
 * Cycle de vie d'un claim de round (voir {@code AdvanceBracketService} et la migration 019).
 *
 * <p>Le claim est pris dans une transaction indépendante de la transaction métier, pour que
 * la contrainte d'unicité soit visible des concurrents avant le commit. Cette indépendance
 * est nécessaire, mais elle crée une fenêtre : entre le commit du claim et le commit de la
 * création des matchs, les deux peuvent diverger. Ces deux valeurs rendent la fenêtre
 * observable au lieu de la laisser invisible.
 */
public enum RoundAdvancementStatus {

    /**
     * Claim pris, création des matchs non confirmée. État transitoire — normalement de
     * quelques millisecondes. Un {@code PENDING} qui persiste est la trace d'une transaction
     * métier annulée après le claim, donc d'un round réservé pour un travail qui n'aura
     * jamais lieu : c'est ce que le job de réconciliation vient libérer.
     */
    PENDING,

    /**
     * Matchs créés et transaction métier commitée. État terminal : le round existe
     * réellement, le claim a rempli son office et doit être respecté par toute exécution
     * ultérieure (redelivery Kafka comprise).
     */
    DONE
}
