package com.jobtracker.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record NoteResponse(
        UUID id,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
