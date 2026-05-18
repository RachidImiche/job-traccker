package com.jobtracker.auth;

import com.jobtracker.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private User user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("my-256-bit-secret-key-that-is-long-enough", 900000, 7);
        when(user.getEmail()).thenReturn("test@example.com");
        when(user.getId()).thenReturn(java.util.UUID.randomUUID());
    }

    @Test
    void generateAccessToken_returnsNonNullNonEmptyToken() {
        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtService.generateAccessToken(user);

        String email = jwtService.extractEmail(token);

        assertEquals("test@example.com", email);
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        String token = jwtService.generateAccessToken(user);

        assertTrue(jwtService.validateToken(token));
    }

    @Test
    void validateToken_returnsFalseForExpiredToken() throws InterruptedException {
        JwtService expiredJwtService = new JwtService("my-256-bit-secret-key-that-is-long-enough", 0, 7);
        String token = expiredJwtService.generateAccessToken(user);
        Thread.sleep(5);

        assertFalse(jwtService.validateToken(token));
    }

    @Test
    void validateToken_returnsFalseForTamperedToken() {
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwtService.validateToken(tampered));
    }
}
