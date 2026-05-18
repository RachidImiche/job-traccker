package com.jobtracker.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String email,
        String fullName
) {
}
