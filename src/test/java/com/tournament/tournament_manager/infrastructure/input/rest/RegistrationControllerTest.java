package com.tournament.tournament_manager.infrastructure.input.rest;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.tournament.tournament_manager.config.security.JwtAuthenticationFilter;
import com.tournament.tournament_manager.config.security.SecurityConfig;
import com.tournament.tournament_manager.config.security.UserDetailsServiceImpl;
import com.tournament.tournament_manager.domain.port.in.registration.GetRegistrationsUseCase;
import com.tournament.tournament_manager.domain.port.in.registration.RegisterPlayerUseCase;
import com.tournament.tournament_manager.dto.request.registration.CreateRegistrationRequest;
import com.tournament.tournament_manager.domain.model.Player;
import com.tournament.tournament_manager.domain.model.Registration;
import com.tournament.tournament_manager.domain.model.Tournament;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import com.tournament.tournament_manager.infrastructure.input.mapper.RegistrationRestMapper;
import org.springframework.context.annotation.Import;
import com.tournament.tournament_manager.domain.model.PageResult;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
@Import({SecurityConfig.class, RegistrationRestMapper.class})
class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterPlayerUseCase registerPlayerUseCase;
    @MockitoBean
    private GetRegistrationsUseCase getRegistrationsUseCase;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;
    @MockitoBean
    private CacheManager cacheManager;
    @MockitoBean
    private ProxyManager<String> rateLimitProxyManager;
    @MockitoBean
    private MeterRegistry meterRegistry;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    private Registration sampleRegistration() {
        Player player = new Player();
        player.setId(1L);
        Tournament tournament = new Tournament();
        tournament.setId(1L);
        Registration registration = new Registration();
        registration.setId(1L);
        registration.setPlayer(player);
        registration.setTournament(tournament);
        return registration;
    }

    @Test
    void createRegistration_shouldReturn201() throws Exception {
        when(registerPlayerUseCase.registerPlayer(any())).thenReturn(sampleRegistration());

        mockMvc.perform(post("/api/registrations")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRegistrationRequest(1L, 1L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createRegistration_shouldReturn400_whenInvalidBody() throws Exception {
        mockMvc.perform(post("/api/registrations")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRegistrationRequest(null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTournamentRegistrations_shouldReturn200() throws Exception {
        com.tournament.tournament_manager.domain.model.PageRequest pageRequest = com.tournament.tournament_manager.domain.model.PageRequest.of(0, 20);

        when(getRegistrationsUseCase.getTournamentRegistrations(eq(1L), any())).thenReturn(PageResult.of(List.of(sampleRegistration()), 0, 20, 1));

        mockMvc.perform(get("/api/registrations/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1));
    }

    @Test
    void createRegistration_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/registrations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRegistrationRequest(1L, 1L))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTournamentRegistrations_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/registrations/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTournamentRegistrations_shouldReturn200_whenPlayerRole() throws Exception {
        com.tournament.tournament_manager.domain.model.PageRequest pageRequest = com.tournament.tournament_manager.domain.model.PageRequest.of(0, 20);
        when(getRegistrationsUseCase.getTournamentRegistrations(eq(1L), any())).thenReturn(PageResult.of(List.of(sampleRegistration()), 0, 20, 1));
        mockMvc.perform(get("/api/registrations/1")
                        .with(user("player").roles("PLAYER")))
                .andExpect(status().isOk());
    }

    @Test
    void createRegistration_shouldReturn200_whenPlayerRole() throws Exception {
        when(registerPlayerUseCase.registerPlayer(any())).thenReturn(sampleRegistration());
        mockMvc.perform(post("/api/registrations")
                        .with(user("player").roles("PLAYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateRegistrationRequest(1L, 1L))))
                .andExpect(status().isCreated());
    }
}