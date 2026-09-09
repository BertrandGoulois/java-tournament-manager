package com.tournament.tournament_manager.domain.port.out.tournament;

/**
 * Port sortant : réclame le droit de créer un round donné pour un tournoi, de façon
 * atomique et sûre sous concurrence (voir {@code AdvanceBracketService}).
 *
 * <p>Le claim a un cycle de vie en deux temps — {@link #tryClaim} puis
 * {@link #markCompleted} — et non un seul. La raison tient au point 2.2 de la revue : le
 * claim doit être commité <b>avant</b> la création des matchs pour être visible des
 * concurrents, mais il ne devient légitime qu'<b>après</b> elle. Entre les deux, il désigne
 * un round réservé pour un travail pas encore accompli ; si la transaction métier échoue
 * là, un claim en un seul temps resterait en base et bloquerait le tournoi pour toujours.
 */
public interface ClaimRoundAdvancementPort {

    /**
     * Tente de réclamer la création du round {@code round} pour le tournoi
     * {@code tournamentId}. Le claim créé est <b>non confirmé</b> : il faut appeler
     * {@link #markCompleted} une fois les matchs créés.
     *
     * <p>L'implémentation libère automatiquement le claim si la transaction appelante est
     * annulée, de sorte qu'un échec métier laisse le round de nouveau réclamable — c'est ce
     * qui permet au retry Kafka de réussir là où il tombait auparavant sur « round déjà
     * réclamé » et sortait en succès sans rien faire.
     *
     * @param tournamentId identifiant du tournoi
     * @param round        numéro du round à réclamer
     * @return {@code true} si la réclamation a réussi (le round n'avait pas encore été créé
     *         ni réclamé par une autre transaction) ; {@code false} si un round identique a
     *         déjà été réclamé — par ce même appel en doublon (redelivery Kafka) ou par une
     *         transaction concurrente.
     */
    boolean tryClaim(Long tournamentId, int round);

    /**
     * Confirme un claim : le round a bien été créé. À appeler dans la <b>même</b> transaction
     * que la création des matchs, pour que les deux réussissent ou échouent ensemble.
     *
     * <p>Sans cet appel, le claim reste non confirmé et le job de réconciliation finira par
     * le libérer, autorisant la recréation d'un round qui existe déjà.
     */
    void markCompleted(Long tournamentId, int round);
}
