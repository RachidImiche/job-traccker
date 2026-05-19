package com.jobtracker.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateNoteRequest(
        @NotBlank(message = "Note content is required")
        @Size(max = 2000, message = "Note content must be at most 2000 characters")
        String content
) {
}
