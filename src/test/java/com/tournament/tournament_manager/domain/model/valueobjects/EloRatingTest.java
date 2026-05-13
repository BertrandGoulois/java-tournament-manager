package com.tournament.tournament_manager.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EloRatingTest {

    @Test
    void shouldThrow_whenValueIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> new EloRating(-1));
    }

    @Test
    void shouldCreate_whenValueIsZero() {
        EloRating rating = new EloRating(0);
        assertEquals(0, rating.value());
    }

    @Test
    void add_shouldNotGoBelowMin() {
        EloRating rating = new EloRating(10);
        EloRating result = rating.add(-100);
        assertEquals(0, result.value());
    }
}