package com.tournament.tournament_manager.application.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tournament.tournament_manager.domain.port.out.rpc.JsonRpcMethodHandler;
import com.tournament.tournament_manager.dto.request.rpc.JsonRpcRequest;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcError;
import com.tournament.tournament_manager.dto.response.rpc.JsonRpcResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonRpcDispatchServiceTest {

    private JsonRpcDispatchService dispatchService;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
                Map<String, Object> map = new ObjectMapper().convertValue(params, Map.class);
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

        dispatchService = new JsonRpcDispatchService(List.of(handler, failingHandler, invalidParamsHandler));
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
    void dispatch_shouldReturnInternalError_whenHandlerThrowsException() throws Exception {
        JsonNode params = objectMapper.readTree("{}");
        JsonRpcRequest request = new JsonRpcRequest("2.0", "test.fail", params, "3");

        JsonRpcResponse response = dispatchService.dispatch(request);

        assertNull(response.result());
        assertNotNull(response.error());
        assertEquals(JsonRpcError.INTERNAL_ERROR, response.error().code());
        assertEquals("something went wrong", response.error().data());
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