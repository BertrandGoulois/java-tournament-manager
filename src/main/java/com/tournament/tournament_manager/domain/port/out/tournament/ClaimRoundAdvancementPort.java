package com.tournament.tournament_manager.domain.port.out.tournament;

/**
 * Port sortant : réclame le droit de créer un round donné pour un tournoi, de façon
 * atomique et sûre sous concurrence (voir {@code AdvanceBracketService}).
 */
public interface ClaimRoundAdvancementPort {

    /**
     * Tente de réclamer la création du round {@code round} pour le tournoi
     * {@code tournamentId}.
     *
     * @param tournamentId identifiant du tournoi
     * @param round        numéro du round à réclamer
     * @return {@code true} si la réclamation a réussi (le round n'avait pas encore été créé
     *         ni réclamé par une autre transaction) ; {@code false} si un round identique a
     *         déjà été réclamé — par ce même appel en doublon (redelivery Kafka) ou par une
     *         transaction concurrente.
     */
    boolean tryClaim(Long tournamentId, int round);
}
