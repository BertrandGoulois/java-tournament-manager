package com.tournament.tournament_manager.infrastructure.input.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tournament.tournament_manager.config.security.JwtAuthenticationFilter;
import com.tournament.tournament_manager.domain.port.in.player.CreatePlayerUseCase;
import com.tournament.tournament_manager.domain.port.in.player.DeletePlayerUseCase;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerStatsUseCase;
import com.tournament.tournament_manager.domain.port.in.player.GetPlayerUseCase;
import com.tournament.tournament_manager.dto.request.player.CreatePlayerRequest;
import com.tournament.tournament_manager.dto.response.player.PlayerResponse;
import com.tournament.tournament_manager.dto.response.player.PlayerStatsResponse;
import com.tournament.tournament_manager.exception.domain.PlayerNotFoundException;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = PlayerController.class, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
})
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreatePlayerUseCase createPlayerUseCase;
    @MockitoBean
    private GetPlayerUseCase getPlayerUseCase;
    @MockitoBean
    private GetPlayerStatsUseCase getPlayerStatsUseCase;
    @MockitoBean
    private DeletePlayerUseCase deletePlayerUseCase;
    @MockitoBean
    private CacheManager cacheManager;
    @MockitoBean
    private ProxyManager<String> rateLimitProxyManager;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private PlayerResponse samplePlayer() {
        return new PlayerResponse(1L, "player1", "player1@mail.com", 1000, LocalDateTime.now());
    }

    @BeforeEach
    void setUp() throws Exception {
        @SuppressWarnings("unchecked")
        RemoteBucketBuilder<String> bucketBuilder =
                mock(RemoteBucketBuilder.class);
        BucketProxy bucket = mock(BucketProxy.class);
        doReturn(bucketBuilder).when(rateLimitProxyManager).builder();
        doReturn(bucket).when(bucketBuilder).build(
                ArgumentMatchers.<String>any(),
                ArgumentMatchers.<Supplier<BucketConfiguration>>any()
        );
        doReturn(true).when(bucket).tryConsume(1L);
    }

    @Test
    void createPlayer_shouldReturn201() throws Exception {
        when(createPlayerUseCase.createPlayer(any())).thenReturn(samplePlayer());

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
        Page<PlayerResponse> page = new PageImpl<>(List.of(samplePlayer()));
        when(getPlayerUseCase.getAllPlayers(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/players").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("player1"));
    }

    @Test
    void getPlayerById_shouldReturn200() throws Exception {
        when(getPlayerUseCase.getPlayerById(1L)).thenReturn(samplePlayer());

        mockMvc.perform(get("/api/players/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getPlayerById_shouldReturn404_whenNotFound() throws Exception {
        when(getPlayerUseCase.getPlayerById(99L)).thenThrow(new PlayerNotFoundException(99L));

        mockMvc.perform(get("/api/players/99").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPlayerStats_shouldReturn200() throws Exception {
        PlayerStatsResponse stats = new PlayerStatsResponse(1L, "player1", 1000, 3, 2, 1, 66.67, List.of());
        when(getPlayerStatsUseCase.getPlayerStats(1L)).thenReturn(stats);

        mockMvc.perform(get("/api/players/1/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("player1"));
    }

    @Test
    void getPlayerStats_shouldReturn404_whenNotFound() throws Exception {
        when(getPlayerStatsUseCase.getPlayerStats(99L)).thenThrow(new PlayerNotFoundException(99L));

        mockMvc.perform(get("/api/players/99/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePlayer_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/players/1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}