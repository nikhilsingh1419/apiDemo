package com.example.apidemo.service;

import com.example.apidemo.config.AuthProperties;
import com.example.apidemo.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final AuthProperties authProperties;
    private SecretKey secretKey;

    public JwtService(AuthProperties authProperties) {
        this.authProperties = authProperties;
    }

    @PostConstruct
    void init() {
        String secret = authProperties.getJwtSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set JWT_SECRET environment variable.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + authProperties.getJwtExpirationMs());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("googleId", user.getGoogleId())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public JwtClaims parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);
        return new JwtClaims(userId, email);
    }

    public long getExpirationSeconds() {
        return authProperties.getJwtExpirationMs() / 1000;
    }

    public record JwtClaims(Long userId, String email) {
    }
}
