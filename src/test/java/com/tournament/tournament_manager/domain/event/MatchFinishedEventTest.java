package com.tournament.tournament_manager.domain.event;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Vérifie que la présence du constructeur de convenance à un seul argument
 * n'empêche pas Jackson de désérialiser normalement le JSON complet (voir
 * {@code @JsonIgnore} sur ce constructeur, et FasterXML/jackson-databind#3968).
 */
class MatchFinishedEventTest {

    private final tools.jackson.databind.ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void shouldDeserialize_fullPayload_withoutThrowing() {
        String json = "{\"matchId\":1,\"player1EloBefore\":1200,\"player2EloBefore\":1000}";

        MatchFinishedEvent event = assertDoesNotThrow(
                () -> objectMapper.readValue(json, MatchFinishedEvent.class));

        assertEquals(1L, event.matchId());
        assertEquals(1200, event.player1EloBefore());
        assertEquals(1000, event.player2EloBefore());
    }

    @Test
    void shouldRoundTrip_serializeThenDeserialize() {
        MatchFinishedEvent original = new MatchFinishedEvent(7L, 1050, 950);

        String json = objectMapper.writeValueAsString(original);
        MatchFinishedEvent roundTripped = objectMapper.readValue(json, MatchFinishedEvent.class);

        assertEquals(original, roundTripped);
    }
}
