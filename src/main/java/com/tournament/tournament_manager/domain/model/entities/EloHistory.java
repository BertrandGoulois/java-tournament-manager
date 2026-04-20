package com.tournament.tournament_manager.domain.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "elo_history")
@Getter
@Setter
@NoArgsConstructor
public class EloHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int eloChange;

    @Column(nullable = false)
    private int eloAfter;

    @Column(nullable=false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne()
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne()
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
