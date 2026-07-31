package com.tournament.tournament_manager.application.rpc;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.tournament.tournament_manager.domain.port.out.rpc.JsonRpcMethodHandler;
import com.tournament.tournament_manager.dto.request.rpc.JsonRpcRequest;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcError;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcResponse;
import com.tournament.tournament_manager.exception.domain.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonRpcDispatchServiceTest {

    private JsonRpcDispatchService dispatchService;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        JsonRpcMethodHandler handler = new JsonRpcMethodHandler() {
            @Override
            public String methodName() {
                return "test.hello";
            }

            @Override
            public Object handle(Object params) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = JsonMapper.builder().build().convertValue(params, Map.class);
                return "hello " + map.get("name");
            }
        };

        JsonRpcMethodHandler failingHandler = new JsonRpcMethodHandler() {
            @Override
            public String methodName() {
                return "test.fail";
            }

            @Override
            public Object handle(Object params) {
                throw new RuntimeException("something went wrong");
            }
        };

        JsonRpcMethodHandler invalidParamsHandler = new JsonRpcMethodHandler() {
            @Override
            public String methodName() {
                return "test.invalidParams";
            }

            @Override
            public Object handle(Object params) {
                throw new IllegalArgumentException("bad param");
            }
        };

        JsonRpcMethodHandler businessFailingHandler = new JsonRpcMethodHandler() {
            @Override
            public String methodName() {
                return "test.notFound";
            }

            @Override
            public Object handle(Object params) {
                throw new NotFoundException("Joueur 42 introuvable");
            }
        };

        dispatchService = new JsonRpcDispatchService(
                List.of(handler, failingHandler, invalidParamsHandler, businessFailingHandler));
    }

    @Test
    void dispatch_shouldReturnSuccess_whenMethodExists() throws Exception {
        JsonNode params = objectMapper.readTree("{\"name\": \"world\"}");
        JsonRpcRequest request = new JsonRpcRequest("2.0", "test.hello", params, "1");

        JsonRpcResponse response = dispatchService.dispatch(request);

        assertNull(response.error());
        assertEquals("hello world", response.result());
        assertEquals("1", response.id());
        assertEquals("2.0", response.jsonrpc());
    }

    @Test
    void dispatch_shouldReturnMethodNotFound_whenMethodUnknown() throws Exception {
        JsonNode params = objectMapper.readTree("{}");
        JsonRpcRequest request = new JsonRpcRequest("2.0", "unknown.method", params, "2");

        JsonRpcResponse response = dispatchService.dispatch(request);

        assertNull(response.result());
        assertNotNull(response.error());
        assertEquals(JsonRpcError.METHOD_NOT_FOUND, response.error().code());
    }

    @Test
    void dispatch_shouldMaskTechnicalMessage_whenHandlerThrowsUnexpectedException() throws Exception {
        JsonNode params = objectMapper.readTree("{}");
        JsonRpcRequest request = new JsonRpcRequest("2.0", "test.fail", params, "3");

        JsonRpcResponse response = dispatchService.dispatch(request);

        assertNull(response.result());
        assertNotNull(response.error());
        assertEquals(JsonRpcError.INTERNAL_ERROR, response.error().code());
        // Le message technique brut ("something went wrong", potentiellement une trace
        // Hibernate/JDBC dans un cas réel) ne doit jamais atteindre le client — seul un
        // message générique est exposé, même politique que GlobalExceptionHandler côté REST.
        assertEquals("Une erreur inattendue s'est produite", response.error().data());
        assertNotEquals("something went wrong", response.error().data());
    }

    @Test
    void dispatch_shouldExposeCuratedMessage_whenHandlerThrowsBusinessException() throws Exception {
        JsonNode params = objectMapper.readTree("{}");
        JsonRpcRequest request = new JsonRpcRequest("2.0", "test.notFound", params, "5");

        JsonRpcResponse response = dispatchService.dispatch(request);

        assertNull(response.result());
        assertNotNull(response.error());
        assertEquals(JsonRpcError.INTERNAL_ERROR, response.error().code());
        // Les exceptions métier curées (NotFoundException, AlreadyExistsException,
        // InvalidException) restent exposées telles quelles : leur message est écrit
        // pour l'appelant, contrairement aux exceptions techniques.
        assertEquals("Joueur 42 introuvable", response.error().data());
    }

    @Test
    void dispatch_shouldReturnInvalidParams_whenHandlerThrowsIllegalArgumentException() throws Exception {
        JsonNode params = objectMapper.readTree("{}");
        JsonRpcRequest request = new JsonRpcRequest("2.0", "test.invalidParams", params, "4");

        JsonRpcResponse response = dispatchService.dispatch(request);

        assertNull(response.result());
        assertNotNull(response.error());
        assertEquals(JsonRpcError.INVALID_PARAMS, response.error().code());
    }

    @Test
    void dispatch_shouldPreserveRequestId_inResponse() throws Exception {
        JsonNode params = objectMapper.readTree("{\"name\": \"test\"}");
        JsonRpcRequest request = new JsonRpcRequest("2.0", "test.hello", params, "my-id-123");

        JsonRpcResponse response = dispatchService.dispatch(request);

        assertEquals("my-id-123", response.id());
    }
}