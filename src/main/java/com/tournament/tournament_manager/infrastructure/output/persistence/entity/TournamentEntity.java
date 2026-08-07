package com.tournament.tournament_manager.infrastructure.output.persistence.entity;

import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA pour la persistance d'un tournoi.
 *
 * <p>Contrepartie technique du domaine pur {@code domain.model.Tournament} — voir
 * {@code TournamentMapper} pour la conversion entre les deux.
 */
@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
@SQLRestriction("deleted = false")
public class TournamentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status = TournamentStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentFormat format = TournamentFormat.SINGLE_ELIMINATION;

    @Column(name = "number_of_groups")
    private Integer numberOfGroups;

    @Column(name = "qualifiers_per_group")
    private Integer qualifiersPerGroup;

    @Column(nullable = false)
    private int maxPlayers;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column
    private Instant deletedAt;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistrationEntity> registrations = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchEntity> matches = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
