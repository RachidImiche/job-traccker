package com.jobtracker.auth.api;

import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email
) {
}
