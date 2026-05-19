package com.jobtracker.application.dto;

import com.jobtracker.application.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ApplicationResponse(
        UUID id,
        UUID userId,
        String companyName,
        String roleTitle,
        String jobUrl,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String location,
        ApplicationStatus status,
        LocalDateTime appliedAt,
        LocalDateTime createdAt
) {
}
