package com.jobtracker.application.dto;

import com.jobtracker.application.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "Status is required")
        ApplicationStatus status
) {
}
