package com.jobtracker.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateApplicationRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 255, message = "Company name must be at most 255 characters")
        String companyName,

        @NotBlank(message = "Role title is required")
        @Size(max = 255, message = "Role title must be at most 255 characters")
        String roleTitle,

        String jobUrl,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String location
) {
}
