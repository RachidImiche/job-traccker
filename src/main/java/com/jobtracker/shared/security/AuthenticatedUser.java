package com.jobtracker.shared.security;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email
) {
}
