package com.jobtracker.auth;

import com.jobtracker.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpiryMs;
    private final long refreshExpiryDays;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiry-ms}") long accessExpiryMs,
            @Value("${jwt.refresh-token-expiry-days}") long refreshExpiryDays
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiryMs = accessExpiryMs;
        this.refreshExpiryDays = refreshExpiryDays;
    }

    public String generateAccessToken(User user) {
        return buildToken(user, accessExpiryMs, "access");
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, Duration.ofDays(refreshExpiryDays).toMillis(), "refresh");
    }

    public boolean validateToken(String token) {
        return extractClaims(token) != null;
    }

    public boolean validateAccessToken(String token) {
        return hasType(token, "access");
    }

    public boolean validateRefreshToken(String token) {
        return hasType(token, "refresh");
    }

    public String extractEmail(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) {
            return null;
        }
        return claims.getSubject();
    }

    public UUID extractUserId(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) {
            return null;
        }

        String userId = claims.get("userId", String.class);
        if (userId == null) {
            return null;
        }

        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            log.debug("Invalid userId claim in JWT token: {}", e.getMessage());
            return null;
        }
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }

    private boolean hasType(String token, String expectedType) {
        Claims claims = extractClaims(token);
        if (claims == null) {
            return false;
        }

        return Objects.equals(expectedType, claims.get("type", String.class));
    }

    private String buildToken(User user, long expiryMs, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .subject(user.getEmail())
                .id(UUID.randomUUID().toString())
                .claim("userId", user.getId().toString())
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
