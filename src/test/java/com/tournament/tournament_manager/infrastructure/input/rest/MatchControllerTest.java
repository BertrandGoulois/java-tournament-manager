package com.tournament.tournament_manager.infrastructure.input.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tournament.tournament_manager.config.security.JwtAuthenticationFilter;
import com.tournament.tournament_manager.config.security.SecurityConfig;
import com.tournament.tournament_manager.config.security.UserDetailsServiceImpl;
import com.tournament.tournament_manager.domain.model.enums.MatchStatus;
import com.tournament.tournament_manager.domain.port.in.match.GetMatchCommentaryUseCase;
import com.tournament.tournament_manager.domain.port.in.match.GetMatchUseCase;
import com.tournament.tournament_manager.domain.port.in.match.RecordMatchResultUseCase;
import com.tournament.tournament_manager.dto.request.match.RecordMatchResultRequest;
import com.tournament.tournament_manager.dto.response.match.MatchCommentaryResponse;
import com.tournament.tournament_manager.dto.response.match.MatchResponse;
import com.tournament.tournament_manager.exception.domain.MatchNotFoundException;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchController.class)
@Import(SecurityConfig.class)
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private RecordMatchResultUseCase recordMatchResultUseCase;
    @MockitoBean
    private GetMatchUseCase getMatchUseCase;
    @MockitoBean
    private GetMatchCommentaryUseCase getMatchCommentaryUseCase;
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

    private MatchResponse sampleMatch() {
        return new MatchResponse(1L, 1, MatchStatus.PENDING, null, 1L, 1L, 2L, null);
    }

    @Test
    void getMatchById_shouldReturn200() throws Exception {
        when(getMatchUseCase.getMatchById(1L)).thenReturn(sampleMatch());

        mockMvc.perform(get("/api/matches/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getMatchById_shouldReturn404_whenNotFound() throws Exception {
        when(getMatchUseCase.getMatchById(99L)).thenThrow(new MatchNotFoundException(99L));

        mockMvc.perform(get("/api/matches/99").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void recordMatchResult_shouldReturn200() throws Exception {
        MatchResponse finished = new MatchResponse(1L, 1, MatchStatus.FINISHED, null, 1L, 1L, 2L, 1L);
        when(recordMatchResultUseCase.recordMatchResult(eq(1L), any())).thenReturn(finished);

        mockMvc.perform(put("/api/matches/1/result")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordMatchResultRequest(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winnerId").value(1));
    }

    @Test
    void recordMatchResult_shouldReturn400_whenInvalidBody() throws Exception {
        mockMvc.perform(put("/api/matches/1/result")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordMatchResultRequest(null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMatchCommentary_shouldReturn200() throws Exception {
        when(getMatchCommentaryUseCase.getMatchCommentary(1L))
                .thenReturn(new MatchCommentaryResponse(1L, "Super match !"));

        mockMvc.perform(get("/api/matches/1/commentary").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentary").value("Super match !"));
    }
}