package com.tournament.tournament_manager.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record EloRating(int value) {

    public static final int DEFAULT = 1000;
    public static final int MIN = 0;

    public EloRating {
        if (value < MIN) {
            throw new IllegalArgumentException("EloRating cannot be negative: " + value);
        }
    }

    public EloRating add(int delta) {
        return new EloRating(Math.max(MIN, value + delta));
    }

    public static EloRating defaultRating() {
        return new EloRating(DEFAULT);
    }
}
