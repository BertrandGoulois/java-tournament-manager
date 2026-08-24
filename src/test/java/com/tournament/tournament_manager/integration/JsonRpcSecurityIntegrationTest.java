package com.tournament.tournament_manager.integration;

import com.tournament.tournament_manager.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie que {@code @PreAuthorize} s'applique réellement sur les 5 handlers JSON-RPC
 * réservés ADMIN, avec un contexte Spring complet (contrairement à
 * {@code JsonRpcControllerTest}, qui mocke {@code JsonRpcDispatchService} et ne peut donc
 * jamais exercer les vraies annotations de sécurité posées sur les handlers).
 *
 * <p>C'est le test qui prouve la correction du point 25 : avant, {@code POST /api/rpc}
 * exigeait ADMIN pour absolument tout ; un joueur normal ne pouvait rien y faire, y compris
 * consulter la liste des joueurs — alors que l'équivalent REST ({@code GET /api/players})
 * ne demandait qu'une authentification.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class JsonRpcSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void tournamentCreate_shouldReturn403_whenPlayerRole() throws Exception {
        mockMvc.perform(post("/api/rpc")
                        .with(user("player23").roles("PLAYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc": "2.0", "method": "tournament.create",
                                 "params": {"name": "X", "maxPlayers": 4, "format": "SINGLE_ELIMINATION"}, "id": "1"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(-32001));
    }

    @Test
    void playerGetAll_shouldReturn200_whenPlayerRole() throws Exception {
        // Contrairement à l'ancienne règle en bloc (ADMIN pour tout /api/rpc), un joueur
        // normal peut désormais appeler les méthodes de lecture, exactement comme côté REST.
        mockMvc.perform(post("/api/rpc")
                        .with(user("player23b").roles("PLAYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc": "2.0", "method": "player.getAll", "params": {}, "id": "1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void tournamentCreate_shouldReturn200_whenAdminRole() throws Exception {
        mockMvc.perform(post("/api/rpc")
                        .with(user("admin23").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc": "2.0", "method": "tournament.create",
                                 "params": {"name": "Point25 RPC Security Test", "maxPlayers": 4, "format": "SINGLE_ELIMINATION"}, "id": "1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").doesNotExist());
    }
}