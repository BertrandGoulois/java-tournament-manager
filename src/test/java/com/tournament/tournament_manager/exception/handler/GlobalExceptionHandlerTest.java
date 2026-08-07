package com.tournament.tournament_manager.exception.handler;

import com.tournament.tournament_manager.exception.domain.AlreadyExistsException;
import com.tournament.tournament_manager.exception.domain.InvalidException;
import com.tournament.tournament_manager.exception.domain.NotFoundException;
import com.tournament.tournament_manager.exception.domain.OpenAiUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_shouldReturn404() {
        ResponseEntity<ErrorResponse> response = handler.handleNotFound(
                new NotFoundException("Player not found") {});
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
    }

    @Test
    void handleAlreadyExists_shouldReturn409() {
        ResponseEntity<ErrorResponse> response = handler.handleAlreadyExists(
                new AlreadyExistsException("Already exists") {});
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
    }

    @Test
    void handleInvalid_shouldReturn400() {
        ResponseEntity<ErrorResponse> response = handler.handleInvalid(
                new InvalidException("Invalid input"));
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody().status());
        assertEquals("Invalid input", response.getBody().message());
    }

    @Test
    void handleValidation_shouldReturn400_withFieldName() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "object");
        bindingResult.addError(new FieldError("object", "username", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("username : must not be blank", response.getBody().message());
    }

    @Test
    void handleOpenAiUnavailable_shouldReturn503() {
        ResponseEntity<ErrorResponse> response = handler.handleOpenAiUnavailable(
                new OpenAiUnavailableException("OpenAI down", new RuntimeException()));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals(503, response.getBody().status());
    }

    @Test
    void handleOptimisticLocking_shouldReturn409() {
        ResponseEntity<ErrorResponse> response = handler.handleOptimisticLocking(
                new ObjectOptimisticLockingFailureException(
                        com.tournament.tournament_manager.domain.model.Player.class, 1L));
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertNotNull(response.getBody().message());
    }

    @Test
    void handleBadCredentials_shouldReturn401() {
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(
                new BadCredentialsException("Bad credentials"));
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
        assertEquals("Identifiants invalides", response.getBody().message());
    }

    @Test
    void handleGeneric_shouldReturn500() {
        ResponseEntity<ErrorResponse> response = handler.handleGeneric(
                new RuntimeException("Unexpected error"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().status());
        assertNotNull(response.getBody().timestamp());
    }
}