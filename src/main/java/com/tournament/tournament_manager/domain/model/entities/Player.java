package com.tournament.tournament_manager.domain.model.entities;

import com.tournament.tournament_manager.domain.model.valueobjects.EloRating;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@SQLRestriction("deleted = false")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "elo_rating", nullable = false))
    private EloRating eloRating = EloRating.defaultRating();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column
    private Instant deletedAt;

    /**
     * Date d'anonymisation, si ce joueur a un historique de matchs/inscriptions et a donc
     * été anonymisé plutôt que supprimé physiquement lors de la purge (voir
     * {@code PlayerRepository.anonymizeWithHistory} et {@code PurgeService}).
     * {@code null} tant qu'il n'a pas (encore) été traité.
     */
    @Column
    private Instant anonymizedAt;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Registration> registrations = new ArrayList<>();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EloHistory> eloHistory = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}