package com.tournament.tournament_manager.infrastructure.input.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tournament.tournament_manager.config.security.JwtAuthenticationFilter;
import com.tournament.tournament_manager.config.security.SecurityConfig;
import com.tournament.tournament_manager.config.security.UserDetailsServiceImpl;
import com.tournament.tournament_manager.domain.model.enums.TournamentFormat;
import com.tournament.tournament_manager.domain.model.enums.TournamentStatus;
import com.tournament.tournament_manager.domain.port.in.tournament.*;
import com.tournament.tournament_manager.dto.request.tournament.CreateTournamentRequest;
import com.tournament.tournament_manager.dto.response.tournament.BracketResponse;
import com.tournament.tournament_manager.dto.response.tournament.StandingsResponse;
import com.tournament.tournament_manager.dto.response.tournament.TournamentResponse;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TournamentController.class)
@Import(SecurityConfig.class)
class TournamentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateTournamentUseCase createTournamentUseCase;
    @MockitoBean
    private GetTournamentUseCase getTournamentUseCase;
    @MockitoBean
    private StartTournamentUseCase startTournamentUseCase;
    @MockitoBean
    private GetBracketUseCase getBracketUseCase;
    @MockitoBean
    private DeleteTournamentUseCase deleteTournamentUseCase;
    @MockitoBean
    private GetStandingsUseCase getStandingsUseCase;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;
    @MockitoBean
    private CacheManager cacheManager;
    @MockitoBean
    private ProxyManager<String> rateLimitProxyManager;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private TournamentResponse sampleTournament() {
        return new TournamentResponse(1L, "Spring Championship", TournamentStatus.OPEN, TournamentFormat.SINGLE_ELIMINATION, 8, null, null, null);
    }

    @Test
    void createTournament_shouldReturn201() throws Exception {
        when(createTournamentUseCase.createTournament(any())).thenReturn(sampleTournament());

        mockMvc.perform(post("/api/tournaments")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTournamentRequest("Spring Championship", 8, TournamentFormat.SINGLE_ELIMINATION, null, null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Spring Championship"));
    }

    @Test
    void createTournament_shouldReturn400_whenInvalidBody() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateTournamentRequest("", 2, TournamentFormat.SINGLE_ELIMINATION, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllTournaments_shouldReturn200() throws Exception {
        Page<TournamentResponse> page = new PageImpl<>(List.of(sampleTournament()));
        when(getTournamentUseCase.getAllTournaments(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/tournaments").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Spring Championship"));
    }

    @Test
    void getTournamentById_shouldReturn200() throws Exception {
        when(getTournamentUseCase.getTournamentById(1L)).thenReturn(sampleTournament());

        mockMvc.perform(get("/api/tournaments/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void startTournament_shouldReturn200() throws Exception {
        mockMvc.perform(post("/api/tournaments/1/start")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    void getBracket_shouldReturn200() throws Exception {
        BracketResponse bracket = new BracketResponse(1L, "Spring Championship", TournamentStatus.OPEN, List.of());
        when(getBracketUseCase.getBracket(1L)).thenReturn(bracket);

        mockMvc.perform(get("/api/tournaments/1/bracket").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tournamentId").value(1))
                .andExpect(jsonPath("$.tournamentName").value("Spring Championship"));
    }

    @Test
    void getStandings_shouldReturn200() throws Exception {
        StandingsResponse standings = new StandingsResponse(1L, "Spring Championship", List.of());
        when(getStandingsUseCase.getStandings(1L)).thenReturn(standings);

        mockMvc.perform(get("/api/tournaments/1/standings").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tournamentId").value(1))
                .andExpect(jsonPath("$.tournamentName").value("Spring Championship"));
    }

    @Test
    void deleteTournament_shouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/tournaments/1")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void createTournament_shouldReturn403_whenPlayerRole() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .with(user("player").roles("PLAYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTournamentRequest("Test", 8, TournamentFormat.SINGLE_ELIMINATION, null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void createTournament_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/tournaments")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateTournamentRequest("Test", 8, TournamentFormat.SINGLE_ELIMINATION, null, null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteTournament_shouldReturn403_whenPlayerRole() throws Exception {
        mockMvc.perform(delete("/api/tournaments/1")
                        .with(user("player").roles("PLAYER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void startTournament_shouldReturn403_whenPlayerRole() throws Exception {
        mockMvc.perform(post("/api/tournaments/1/start")
                        .with(user("player").roles("PLAYER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllTournaments_shouldReturn200_whenPlayerRole() throws Exception {
        when(getTournamentUseCase.getAllTournaments(any())).thenReturn(Page.empty());
        mockMvc.perform(get("/api/tournaments")
                        .with(user("player").roles("PLAYER")))
                .andExpect(status().isOk());
    }
}