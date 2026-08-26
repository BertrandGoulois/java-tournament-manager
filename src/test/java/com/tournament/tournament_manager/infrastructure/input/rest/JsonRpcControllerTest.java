package com.tournament.tournament_manager.infrastructure.input.rest;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.tournament.tournament_manager.application.rpc.JsonRpcDispatchService;
import com.tournament.tournament_manager.config.security.JwtAuthenticationFilter;
import com.tournament.tournament_manager.config.security.SecurityConfig;
import com.tournament.tournament_manager.config.security.UserDetailsServiceImpl;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcError;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcResponse;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste le contrôleur en isolation (dispatch mocké) : la responsabilité vérifiée ici est le
 * transport (batch, notifications, validation du champ {@code jsonrpc}, mapping des codes
 * d'erreur vers des statuts HTTP) — pas la logique métier de dispatch elle-même
 * ({@code JsonRpcDispatchServiceTest}), ni l'application réelle de {@code @PreAuthorize} sur
 * les handlers ADMIN (nécessite un contexte Spring complet, voir
 * {@code JsonRpcSecurityIT}).
 */
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
        // METHOD_NOT_FOUND reste 200 : réponse protocolaire bien formée, pas une panne
        // de transport (voir la Javadoc de JsonRpcController.httpStatusFor).
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
    void handle_shouldReturn500_whenInternalError() throws Exception {
        // Point 25 : une vraie erreur interne doit maintenant se voir en HTTP (500), pas
        // rester invisible sous un 200 générique — c'est tout l'objet du mapping de statut.
        when(dispatchService.dispatch(any()))
                .thenReturn(JsonRpcResponse.failure(
                        new JsonRpcError(JsonRpcError.INTERNAL_ERROR, "Internal error",
                                "Une erreur inattendue s'est produite"),
                        "3"));

        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc": "2.0", "method": "test.fail", "params": {}, "id": "3"}
                                """))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void handle_shouldReturn409_whenConflict() throws Exception {
        when(dispatchService.dispatch(any()))
                .thenReturn(JsonRpcResponse.failure(
                        new JsonRpcError(JsonRpcError.CONFLICT, "Conflict", "Modifié entre-temps"),
                        "4"));

        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc": "2.0", "method": "match.recordResult", "params": {}, "id": "4"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void handle_shouldReturn400_whenBusinessError() throws Exception {
        when(dispatchService.dispatch(any()))
                .thenReturn(JsonRpcResponse.failure(
                        new JsonRpcError(JsonRpcError.BUSINESS_ERROR, "Request failed", "Introuvable"),
                        "5"));

        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc": "2.0", "method": "player.getById", "params": {}, "id": "5"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void handle_shouldReturn400_whenJsonrpcFieldMissing() throws Exception {
        // Point 25 : le champ jsonrpc doit valoir exactement "2.0" — jamais vérifié avant.
        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"method": "tournament.create", "params": {}, "id": "1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(-32600));
    }

    @Test
    void handle_shouldReturn400_whenJsonrpcFieldWrongVersion() throws Exception {
        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc": "1.0", "method": "tournament.create", "params": {}, "id": "1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(-32600));
    }

    @Test
    void handle_shouldReturn204_withoutCallingBody_whenNotification() throws Exception {
        // Une requête sans id est une notification : exécutée, mais sans réponse renvoyée
        // (spec JSON-RPC 2.0), même en cas de succès.
        when(dispatchService.dispatch(any()))
                .thenReturn(JsonRpcResponse.success("ok", null));

        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jsonrpc": "2.0", "method": "tournament.create", "params": {}}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void handle_shouldProcessBatch_andReturnArrayOfResponses() throws Exception {
        when(dispatchService.dispatch(argThat(r -> r != null && "1".equals(r.id()))))
                .thenReturn(JsonRpcResponse.success("result-1", "1"));
        when(dispatchService.dispatch(argThat(r -> r != null && "2".equals(r.id()))))
                .thenReturn(JsonRpcResponse.success("result-2", "2"));

        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {"jsonrpc": "2.0", "method": "player.getById", "params": {}, "id": "1"},
                                    {"jsonrpc": "2.0", "method": "player.getById", "params": {}, "id": "2"}
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("result-1"))
                .andExpect(jsonPath("$[1].result").value("result-2"));
    }

    @Test
    void handle_shouldOmitNotificationsFromBatchResponse() throws Exception {
        when(dispatchService.dispatch(argThat(r -> r != null && "1".equals(r.id()))))
                .thenReturn(JsonRpcResponse.success("result-1", "1"));
        when(dispatchService.dispatch(argThat(r -> r != null && r.id() == null)))
                .thenReturn(JsonRpcResponse.success("ignored", null));

        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {"jsonrpc": "2.0", "method": "player.getById", "params": {}, "id": "1"},
                                    {"jsonrpc": "2.0", "method": "player.getById", "params": {}}
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("1"));
    }

    @Test
    void handle_shouldReturn204_whenBatchContainsOnlyNotifications() throws Exception {
        when(dispatchService.dispatch(any()))
                .thenReturn(JsonRpcResponse.success("ignored", null));

        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                    {"jsonrpc": "2.0", "method": "player.getById", "params": {}},
                                    {"jsonrpc": "2.0", "method": "player.getById", "params": {}}
                                ]
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    void handle_shouldReturn400_whenBatchIsEmpty() throws Exception {
        mockMvc.perform(post("/api/rpc")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(-32600));
    }
}
