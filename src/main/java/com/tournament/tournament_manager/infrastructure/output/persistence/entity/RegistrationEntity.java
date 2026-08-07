package com.tournament.tournament_manager.infrastructure.output.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Entité JPA pour la persistance d'une inscription.
 *
 * <p>Contrepartie technique du domaine pur {@code domain.model.Registration} — voir
 * {@code RegistrationMapper} pour la conversion entre les deux.
 */
@Entity
@Table(name = "registrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_id", "tournament_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class RegistrationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant registeredAt;

    @ManyToOne()
    @JoinColumn(name = "tournament_id", nullable = false)
    private TournamentEntity tournament;

    @ManyToOne()
    @JoinColumn(name = "player_id", nullable = false)
    private PlayerEntity player;

    @PrePersist
    protected void onCreate() {
        this.registeredAt = Instant.now();
    }
}
