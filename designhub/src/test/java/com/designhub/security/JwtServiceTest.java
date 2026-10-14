package com.designhub.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final long EXPIRATION = 1800000; // 30 minutes

    private JwtService jwtService;
    private String testSecret;

    @BeforeEach
    void setUp() {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        testSecret = Base64.getEncoder().encodeToString(randomBytes);

        jwtService = new JwtService(testSecret, EXPIRATION);
    }

    @Test
    void testGenerateAndExtractClaims() {
        String username = "testuser";
        String role = "CUSTOMER";

        String token = jwtService.generateToken(username, role);

        assertNotNull(token);
        assertEquals(username, jwtService.extractUsername(token));
        assertEquals(role, jwtService.extractRole(token));
        assertTrue(jwtService.isTokenValid(token, username));
        assertFalse(jwtService.isTokenValid(token, "otheruser"));
    }

    @Test
    void testExpiredTokenThrowsException() {
        JwtService expiredJwtService = new JwtService(testSecret, -1000);
        String token = expiredJwtService.generateToken("expiredUser", "ADMIN");

        assertThrows(JwtException.class, () -> expiredJwtService.extractUsername(token));
    }

    @Test
    void testTamperedTokenThrowsException() {
        String token = jwtService.generateToken("user1", "CUSTOMER");
        String tamperedToken = token + "xyz";

        assertThrows(JwtException.class, () -> jwtService.extractUsername(tamperedToken));
    }
}
