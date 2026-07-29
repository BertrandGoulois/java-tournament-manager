package com.tournament.tournament_manager.infrastructure.input.rest;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.tournament.tournament_manager.config.security.JwtAuthenticationFilter;
import com.tournament.tournament_manager.config.security.SecurityConfig;
import com.tournament.tournament_manager.config.security.UserDetailsServiceImpl;
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
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(PlayerController.class)
@Import(SecurityConfig.class)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
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
    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;
    @MockitoBean
    private MeterRegistry meterRegistry;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    private PlayerResponse samplePlayer() {
        return new PlayerResponse(1L, "player1", "player1@mail.com", 1000, LocalDateTime.now());
    }

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        @SuppressWarnings("unchecked")
        RemoteBucketBuilder<String> bucketBuilder = mock(RemoteBucketBuilder.class);
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

    @Test
    void deletePlayer_shouldReturn403_whenPlayerRole() throws Exception {
        mockMvc.perform(delete("/api/players/1")
                        .with(user("player").roles("PLAYER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletePlayer_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/players/1")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllPlayers_shouldReturn200_whenPlayerRole() throws Exception {
        Page<PlayerResponse> page = new PageImpl<>(List.of(samplePlayer()));
        when(getPlayerUseCase.getAllPlayers(any(Pageable.class))).thenReturn(page);
        mockMvc.perform(get("/api/players")
                        .with(user("player").roles("PLAYER")))
                .andExpect(status().isOk());
    }

    @Test
    void getPlayerById_shouldReturn200_whenPlayerRole() throws Exception {
        when(getPlayerUseCase.getPlayerById(1L)).thenReturn(samplePlayer());
        mockMvc.perform(get("/api/players/1")
                        .with(user("player").roles("PLAYER")))
                .andExpect(status().isOk());
    }

    @Test
    void createPlayer_shouldReturn400_whenUsernameTooShort() throws Exception {
        mockMvc.perform(post("/api/players")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePlayerRequest("ab", "valid@mail.com"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlayer_shouldReturn400_whenUsernameHasSpecialChars() throws Exception {
        mockMvc.perform(post("/api/players")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePlayerRequest("player@#$", "valid@mail.com"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPlayer_shouldReturn400_whenUsernameTooLong() throws Exception {
        mockMvc.perform(post("/api/players")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePlayerRequest("a".repeat(31), "valid@mail.com"))))
                .andExpect(status().isBadRequest());
    }
}