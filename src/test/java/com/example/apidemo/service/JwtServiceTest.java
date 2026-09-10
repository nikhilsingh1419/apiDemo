package com.example.apidemo.service;

import com.example.apidemo.config.AuthProperties;
import com.example.apidemo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setJwtSecret("test-jwt-secret-key-with-at-least-32-bytes");
        authProperties.setJwtExpirationMs(3_600_000L);

        jwtService = new JwtService(authProperties);
        jwtService.init();
    }

    @Test
    void generatesAndParsesToken() {
        User user = new User();
        user.setId(42L);
        user.setGoogleSub("google-123");
        user.setEmail("user@example.com");

        String token = jwtService.generateToken(user);
        JwtService.JwtClaims claims = jwtService.parseToken(token);

        assertEquals(42L, claims.userId());
        assertEquals("user@example.com", claims.email());
    }
}
