package com.jobtracker.application.dto;

import java.math.BigDecimal;

public record UpdateApplicationRequest(
        String companyName,
        String roleTitle,
        String jobUrl,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String location
) {
}
