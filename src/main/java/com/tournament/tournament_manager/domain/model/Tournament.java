package com.tournament.tournament_manager.domain.model;

import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.model.valueobjects.TournamentName;
import com.tournament.tournament_manager.exception.domain.InvalidException;

import java.time.Instant;
import java.util.Objects;

/**
 * Un tournoi, tel que le domaine métier le connaît.
 *
 * <p>Objet de domaine pur : aucune annotation, aucune dépendance vers JPA, Spring, ou toute
 * autre librairie technique. La persistance est gérée séparément par
 * {@code infrastructure.output.persistence.entity.TournamentEntity} et
 * {@code infrastructure.output.persistence.mapper.TournamentMapper}.
 *
 * <p>Pas de setters publics : les transitions d'état ({@code OPEN} -> {@code IN_PROGRESS} ->
 * {@code FINISHED}) ne sont possibles qu'au travers de {@link #start()} et {@link #finish()},
 * qui valident la transition plutôt que de l'accepter aveuglément - {@code setStatus(FINISHED)}
 * était auparavant appelable depuis n'importe où, sans qu'aucune règle ne l'empêche d'être
 * appelé sur un tournoi qui n'avait même pas commencé.
 *
 * <p>Deux façons de construire un tournoi : {@link #create} pour un nouveau tournoi (règles
 * métier appliquées dès la construction), {@link #reconstitute} réservé à
 * {@code TournamentMapper} pour recharger un tournoi déjà persisté - cette seconde voie
 * contourne délibérément {@code start()}/{@code finish()} puisque l'état chargé a déjà été
 * validé lors de sa création initiale ; il n'y a pas de sens à revalider une transition qui
 * a déjà eu lieu.
 */
public class Tournament {

    private Long id;
    private TournamentName name;
    private TournamentStatus status = TournamentStatus.OPEN;
    private TournamentFormat format = TournamentFormat.SINGLE_ELIMINATION;
    private Integer numberOfGroups;
    private Integer qualifiersPerGroup;
    private int maxPlayers;
    private Instant createdAt;
    private boolean deleted;
    private Instant deletedAt;

    private Tournament() {
    }

    /**
     * Crée un nouveau tournoi, toujours au statut {@code OPEN}.
     */
    public static Tournament create(TournamentName name, int maxPlayers, TournamentFormat format,
                                    Integer numberOfGroups, Integer qualifiersPerGroup) {
        Tournament tournament = new Tournament();
        tournament.name = name;
        tournament.maxPlayers = maxPlayers;
        tournament.format = format;
        tournament.numberOfGroups = numberOfGroups;
        tournament.qualifiersPerGroup = qualifiersPerGroup;
        return tournament;
    }

    /**
     * Reconstruit un tournoi depuis un état déjà persisté. Réservé à
     * {@code TournamentMapper} - ne jamais appeler depuis un service applicatif.
     */
    public static Tournament reconstitute(Long id, TournamentName name, TournamentStatus status,
                                          TournamentFormat format, Integer numberOfGroups,
                                          Integer qualifiersPerGroup, int maxPlayers,
                                          Instant createdAt, boolean deleted, Instant deletedAt) {
        Tournament tournament = new Tournament();
        tournament.id = id;
        tournament.name = name;
        tournament.status = status;
        tournament.format = format;
        tournament.numberOfGroups = numberOfGroups;
        tournament.qualifiersPerGroup = qualifiersPerGroup;
        tournament.maxPlayers = maxPlayers;
        tournament.createdAt = createdAt;
        tournament.deleted = deleted;
        tournament.deletedAt = deletedAt;
        return tournament;
    }

    /**
     * Démarre le tournoi : {@code OPEN} -> {@code IN_PROGRESS}.
     *
     * @throws InvalidException si le tournoi n'est pas {@code OPEN}
     */
    public void start() {
        if (status != TournamentStatus.OPEN) {
            throw new InvalidException(
                    "Un tournoi ne peut démarrer que s'il est OPEN (statut actuel : " + status + ")");
        }
        status = TournamentStatus.IN_PROGRESS;
    }

    /**
     * Termine le tournoi : {@code IN_PROGRESS} -> {@code FINISHED}.
     *
     * @throws InvalidException si le tournoi n'est pas {@code IN_PROGRESS}
     */
    public void finish() {
        if (status != TournamentStatus.IN_PROGRESS) {
            throw new InvalidException(
                    "Un tournoi ne peut se terminer que s'il est IN_PROGRESS (statut actuel : " + status + ")");
        }
        status = TournamentStatus.FINISHED;
    }

    /**
     * Marque le tournoi comme supprimé (soft delete).
     */
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public TournamentName getName() {
        return name;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public TournamentFormat getFormat() {
        return format;
    }

    public Integer getNumberOfGroups() {
        return numberOfGroups;
    }

    public Integer getQualifiersPerGroup() {
        return qualifiersPerGroup;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tournament tournament)) return false;
        return id != null && id.equals(tournament.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
