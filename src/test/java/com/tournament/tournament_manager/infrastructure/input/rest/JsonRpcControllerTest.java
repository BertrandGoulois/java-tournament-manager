package com.tournament.tournament_manager.infrastructure.input.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.config.security.JwtAuthenticationFilter;
import com.tournament.tournament_manager.config.security.SecurityConfig;
import com.tournament.tournament_manager.config.security.UserDetailsServiceImpl;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcError;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcResponse;
import com.tournament.tournament_manager.application.rpc.JsonRpcDispatchService;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JsonRpcController.class)
@Import(SecurityConfig.class)
class JsonRpcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JsonRpcDispatchService dispatchService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;
    @MockitoBean
    private CacheManager cacheManager;
    @MockitoBean
    private ProxyManager<String> rateLimitProxyManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            jakarta.servlet.FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
    }

    @Test
    void handle_shouldReturn200_withSuccessResponse() throws Exception {
        when(dispatchService.dispatch(any()))
                .thenReturn(JsonRpcResponse.success("ok", "1"));

        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.0",
                                    "method": "tournament.create",
                                    "params": {"name": "Test", "maxPlayers": 8},
                                    "id": "1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.result").value("ok"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.id").value("1"));
    }

    @Test
    void handle_shouldReturn200_withErrorResponse_whenMethodNotFound() throws Exception {
        when(dispatchService.dispatch(any()))
                .thenReturn(JsonRpcResponse.failure(
                        new JsonRpcError(JsonRpcError.METHOD_NOT_FOUND, "Method not found", null),
                        "2"));

        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jsonrpc": "2.0",
                                    "method": "unknown.method",
                                    "params": {},
                                    "id": "2"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonrpc").value("2.0"))
                .andExpect(jsonPath("$.result").doesNotExist())
                .andExpect(jsonPath("$.error.code").value(-32601))
                .andExpect(jsonPath("$.id").value("2"));
    }

    @Test
    void handle_shouldReturn401_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/rpc")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "jsonrpc": "2.0",
                                "method": "tournament.create",
                                "params": {},
                                "id": "1"
                            }
                            """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void handle_shouldReturn403_whenPlayerRole() throws Exception {
        mockMvc.perform(post("/api/rpc")
                        .with(user("player").roles("PLAYER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "jsonrpc": "2.0",
                                "method": "tournament.create",
                                "params": {},
                                "id": "1"
                            }
                            """))
                .andExpect(status().isForbidden());
    }
}