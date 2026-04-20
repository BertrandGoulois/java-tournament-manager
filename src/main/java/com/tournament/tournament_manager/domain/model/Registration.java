package com.tournament.tournament_manager.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@Table(name = "registrations", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_id", "tournament_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @ManyToOne()
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne()
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
    }
}
