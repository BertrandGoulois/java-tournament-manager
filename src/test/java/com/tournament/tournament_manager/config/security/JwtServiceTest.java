package com.tournament.tournament_manager.config.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
    }

    @Test
    void generateToken_shouldReturnToken() {
        String token = jwtService.generateToken("admin");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractUsername_shouldReturnUsername() {
        String token = jwtService.generateToken("admin");
        assertEquals("admin", jwtService.extractUsername(token));
    }

    @Test
    void isTokenValid_shouldReturnTrue_whenValidToken() {
        String token = jwtService.generateToken("admin");
        assertTrue(jwtService.isTokenValid(token, "admin"));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenWrongUsername() {
        String token = jwtService.generateToken("admin");
        assertFalse(jwtService.isTokenValid(token, "other"));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenExpiredToken() throws Exception {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.generateToken("admin");
        assertThrows(io.jsonwebtoken.ExpiredJwtException.class,
                () -> jwtService.isTokenValid(token, "admin"));
    }
}