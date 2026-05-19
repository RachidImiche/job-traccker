package com.jobtracker.application;

import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.shared.exception.AppException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ApplicationService {

    private final JobApplicationRepository jobApplicationRepository;

    public ApplicationService(JobApplicationRepository jobApplicationRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
    }

    @Transactional
    public ApplicationResponse create(UUID userId, CreateApplicationRequest request) {
        JobApplication application = JobApplication.builder()
                .userId(userId)
                .companyName(request.companyName().trim())
                .roleTitle(request.roleTitle().trim())
                .jobUrl(request.jobUrl())
                .salaryMin(request.salaryMin())
                .salaryMax(request.salaryMax())
                .location(request.location())
                .status(ApplicationStatus.APPLIED)
                .appliedAt(LocalDateTime.now())
                .build();

        JobApplication savedApplication = jobApplicationRepository.save(application);
        return toResponse(savedApplication);
    }

    @Transactional(readOnly = true)
    public ApplicationResponse getById(UUID userId, UUID applicationId) {
        JobApplication application = findOwnedApplication(userId, applicationId);
        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public Page<ApplicationResponse> getAll(UUID userId, Pageable pageable) {
        return jobApplicationRepository.findAllByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public ApplicationResponse update(UUID userId, UUID applicationId, UpdateApplicationRequest request) {
        JobApplication application = findOwnedApplication(userId, applicationId);

        if (request.companyName() != null) {
            application.setCompanyName(request.companyName().trim());
        }

        if (request.roleTitle() != null) {
            application.setRoleTitle(request.roleTitle().trim());
        }

        if (request.jobUrl() != null) {
            application.setJobUrl(request.jobUrl());
        }

        if (request.salaryMin() != null) {
            application.setSalaryMin(request.salaryMin());
        }

        if (request.salaryMax() != null) {
            application.setSalaryMax(request.salaryMax());
        }

        if (request.location() != null) {
            application.setLocation(request.location());
        }

        JobApplication savedApplication = jobApplicationRepository.save(application);
        return toResponse(savedApplication);
    }

    @Transactional
    public void delete(UUID userId, UUID applicationId) {
        JobApplication application = findOwnedApplication(userId, applicationId);
        jobApplicationRepository.delete(application);
    }

    private JobApplication findOwnedApplication(UUID userId, UUID applicationId) {
        return jobApplicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private ApplicationResponse toResponse(JobApplication application) {
        return new ApplicationResponse(
                application.getId(),
                application.getUserId(),
                application.getCompanyName(),
                application.getRoleTitle(),
                application.getJobUrl(),
                application.getSalaryMin(),
                application.getSalaryMax(),
                application.getLocation(),
                application.getStatus(),
                application.getAppliedAt(),
                application.getCreatedAt()
        );
    }
}
