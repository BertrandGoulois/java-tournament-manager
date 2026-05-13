package com.tournament.tournament_manager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tournament.tournament_manager.config.security.JwtAuthenticationFilter;
import com.tournament.tournament_manager.dto.request.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.PlayerResponse;
import com.tournament.tournament_manager.dto.response.PlayerStatsResponse;
import com.tournament.tournament_manager.exception.PlayerNotFoundException;
import com.tournament.tournament_manager.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = PlayerController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
})
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @MockitoBean
    private org.springframework.cache.CacheManager cacheManager;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private PlayerResponse samplePlayer() {
        return new PlayerResponse(1L, "player1", "player1@mail.com", 1000, LocalDateTime.now());
    }

    @Test
    void createPlayer_shouldReturn201() throws Exception {
        when(playerService.createPlayer(any())).thenReturn(samplePlayer());

        mockMvc.perform(post("/api/players")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePlayerRequest("player1", "player1@mail.com"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("player1"));
    }

    @Test
    void createPlayer_shouldReturn400_whenInvalidBody() throws Exception {
        mockMvc.perform(post("/api/players")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePlayerRequest("", "notanemail"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllPlayers_shouldReturn200() throws Exception {
        when(playerService.getAllPlayers()).thenReturn(List.of(samplePlayer()));

        mockMvc.perform(get("/api/players").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("player1"));
    }

    @Test
    void getPlayerById_shouldReturn200() throws Exception {
        when(playerService.getPlayerById(1L)).thenReturn(samplePlayer());

        mockMvc.perform(get("/api/players/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getPlayerById_shouldReturn404_whenNotFound() throws Exception {
        when(playerService.getPlayerById(99L)).thenThrow(new PlayerNotFoundException(99L));

        mockMvc.perform(get("/api/players/99").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPlayerStats_shouldReturn200() throws Exception {
        PlayerStatsResponse stats = new PlayerStatsResponse(1L, "player1", 1000, 3, 2, 1, 66.67, List.of());
        when(playerService.getPlayerStats(1L)).thenReturn(stats);

        mockMvc.perform(get("/api/players/1/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("player1"));
    }

    @Test
    void getPlayerStats_shouldReturn404_whenNotFound() throws Exception {
        when(playerService.getPlayerStats(99L)).thenThrow(new PlayerNotFoundException(99L));

        mockMvc.perform(get("/api/players/99/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }
}