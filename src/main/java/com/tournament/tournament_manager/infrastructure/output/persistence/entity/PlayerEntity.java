package com.tournament.tournament_manager.infrastructure.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA pour la persistance d'un joueur.
 *
 * <p>Contrepartie technique du domaine pur {@code domain.model.Player} — voir
 * {@code PlayerMapper} pour la conversion entre les deux. Cette classe ne doit jamais être
 * référencée en dehors de la couche infrastructure (adapters, repositories, mappers) ;
 * les ports et les services applicatifs manipulent exclusivement {@code domain.model.Player}.
 */
@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@SQLRestriction("deleted = false")
public class PlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "elo_rating", nullable = false)
    private int eloRating;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column
    private Instant deletedAt;

    @Column
    private Instant anonymizedAt;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistrationEntity> registrations = new ArrayList<>();

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EloHistoryEntity> eloHistory = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
