package com.jobtracker.application;

import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private StatusTransitionValidator statusTransitionValidator;

    @Mock
    private ApplicationStatusHistoryRepository applicationStatusHistoryRepository;

    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        applicationService = new ApplicationService(
                jobApplicationRepository,
                statusTransitionValidator,
                applicationStatusHistoryRepository
        );
    }

    @Test
    void create_savesApplicationAndReturnsResponse() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        LocalDateTime appliedAt = LocalDateTime.of(2026, 5, 19, 10, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 19, 10, 1);

        CreateApplicationRequest request = new CreateApplicationRequest(
                "  Acme Corp  ",
                "  Backend Engineer  ",
                "https://acme.example/jobs/backend",
                new BigDecimal("100000.00"),
                new BigDecimal("130000.00"),
                "Remote"
        );

        JobApplication saved = new JobApplication();
        saved.setId(applicationId);
        saved.setUserId(userId);
        saved.setCompanyName("Acme Corp");
        saved.setRoleTitle("Backend Engineer");
        saved.setJobUrl("https://acme.example/jobs/backend");
        saved.setSalaryMin(new BigDecimal("100000.00"));
        saved.setSalaryMax(new BigDecimal("130000.00"));
        saved.setLocation("Remote");
        saved.setStatus(ApplicationStatus.APPLIED);
        saved.setAppliedAt(appliedAt);
        saved.setCreatedAt(createdAt);

        when(jobApplicationRepository.save(any(JobApplication.class))).thenReturn(saved);

        ApplicationResponse response = applicationService.create(userId, request);

        ArgumentCaptor<JobApplication> captor = ArgumentCaptor.forClass(JobApplication.class);
        verify(jobApplicationRepository).save(captor.capture());
        JobApplication toSave = captor.getValue();

        assertEquals(userId, toSave.getUserId());
        assertEquals("Acme Corp", toSave.getCompanyName());
        assertEquals("Backend Engineer", toSave.getRoleTitle());
        assertEquals(ApplicationStatus.APPLIED, toSave.getStatus());
        assertNotNull(toSave.getAppliedAt());

        assertEquals(applicationId, response.id());
        assertEquals(userId, response.userId());
        assertEquals("Acme Corp", response.companyName());
        assertEquals("Backend Engineer", response.roleTitle());
        assertEquals("https://acme.example/jobs/backend", response.jobUrl());
        assertEquals(new BigDecimal("100000.00"), response.salaryMin());
        assertEquals(new BigDecimal("130000.00"), response.salaryMax());
        assertEquals("Remote", response.location());
        assertEquals(ApplicationStatus.APPLIED, response.status());
        assertEquals(appliedAt, response.appliedAt());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void getById_returnsOwnedApplication() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        JobApplication application = application(applicationId, userId, "Acme", "Engineer", "Remote");

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.of(application));

        ApplicationResponse response = applicationService.getById(userId, applicationId);

        assertEquals(applicationId, response.id());
        assertEquals(userId, response.userId());
        assertEquals("Acme", response.companyName());
        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
    }

    @Test
    void getById_throwsNotFoundWhenApplicationDoesNotExistOrIsNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> applicationService.getById(userId, applicationId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Application not found", exception.getMessage());
        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
    }

    @Test
    void getAll_returnsMappedPageForUser() {
        UUID userId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(0, 20);

        JobApplication one = application(UUID.randomUUID(), userId, "Acme", "Engineer", "Remote");
        JobApplication two = application(UUID.randomUUID(), userId, "Globex", "SRE", "Berlin");
        Page<JobApplication> page = new PageImpl<>(List.of(one, two), pageable, 2);

        when(jobApplicationRepository.findAllByUserId(userId, pageable)).thenReturn(page);

        Page<ApplicationResponse> responsePage = applicationService.getAll(userId, pageable);

        assertEquals(2, responsePage.getTotalElements());
        assertEquals("Acme", responsePage.getContent().getFirst().companyName());
        assertEquals("Globex", responsePage.getContent().get(1).companyName());
        verify(jobApplicationRepository).findAllByUserId(userId, pageable);
    }

    @Test
    void update_partiallyUpdatesApplicationAndReturnsResponse() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        JobApplication existing = application(applicationId, userId, "Acme", "Engineer", "Remote");

        UpdateApplicationRequest request = new UpdateApplicationRequest(
                "  New Acme  ",
                null,
                "https://new.example/jobs/1",
                null,
                new BigDecimal("180000.00"),
                null
        );

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.of(existing));
        when(jobApplicationRepository.save(existing)).thenReturn(existing);

        ApplicationResponse response = applicationService.update(userId, applicationId, request);

        assertEquals("New Acme", existing.getCompanyName());
        assertEquals("Engineer", existing.getRoleTitle());
        assertEquals("https://new.example/jobs/1", existing.getJobUrl());
        assertEquals(new BigDecimal("100000.00"), existing.getSalaryMin());
        assertEquals(new BigDecimal("180000.00"), existing.getSalaryMax());
        assertEquals("Remote", existing.getLocation());

        assertEquals("New Acme", response.companyName());
        assertEquals("Engineer", response.roleTitle());
        assertEquals("https://new.example/jobs/1", response.jobUrl());
        assertEquals(new BigDecimal("180000.00"), response.salaryMax());
        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
        verify(jobApplicationRepository).save(existing);
    }

    @Test
    void update_throwsNotFoundWhenApplicationDoesNotExistOrIsNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> applicationService.update(userId, applicationId,
                        new UpdateApplicationRequest("Acme", null, null, null, null, null)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Application not found", exception.getMessage());
        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
    }

    @Test
    void delete_removesOwnedApplication() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        JobApplication existing = application(applicationId, userId, "Acme", "Engineer", "Remote");

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.of(existing));

        applicationService.delete(userId, applicationId);

        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
        verify(jobApplicationRepository).delete(existing);
    }

    @Test
    void delete_throwsNotFoundWhenApplicationDoesNotExistOrIsNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> applicationService.delete(userId, applicationId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Application not found", exception.getMessage());
        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
    }

    @Test
    void updateStatus_updatesStatusSavesHistoryAndReturnsResponse() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        JobApplication existing = application(applicationId, userId, "Acme", "Engineer", "Remote");

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.of(existing));
        when(jobApplicationRepository.save(existing)).thenReturn(existing);

        ApplicationResponse response = applicationService.updateStatus(userId, applicationId, ApplicationStatus.INTERVIEW);

        assertEquals(ApplicationStatus.INTERVIEW, existing.getStatus());
        assertEquals(ApplicationStatus.INTERVIEW, response.status());

        verify(statusTransitionValidator).validate(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW);
        verify(jobApplicationRepository).save(existing);

        ArgumentCaptor<ApplicationStatusHistory> historyCaptor = ArgumentCaptor.forClass(ApplicationStatusHistory.class);
        verify(applicationStatusHistoryRepository).save(historyCaptor.capture());
        ApplicationStatusHistory history = historyCaptor.getValue();

        assertEquals(applicationId, history.getApplicationId());
        assertEquals(ApplicationStatus.APPLIED, history.getOldStatus());
        assertEquals(ApplicationStatus.INTERVIEW, history.getNewStatus());
        assertNotNull(history.getChangedAt());
    }

    @Test
    void updateStatus_throwsNotFoundWhenApplicationDoesNotExistOrIsNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> applicationService.updateStatus(userId, applicationId, ApplicationStatus.INTERVIEW));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Application not found", exception.getMessage());
        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
        verifyNoInteractions(statusTransitionValidator);
        verifyNoInteractions(applicationStatusHistoryRepository);
    }

    private JobApplication application(UUID id, UUID userId, String companyName, String roleTitle, String location) {
        JobApplication application = new JobApplication();
        application.setId(id);
        application.setUserId(userId);
        application.setCompanyName(companyName);
        application.setRoleTitle(roleTitle);
        application.setJobUrl("https://example.com/job/1");
        application.setSalaryMin(new BigDecimal("100000.00"));
        application.setSalaryMax(new BigDecimal("150000.00"));
        application.setLocation(location);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setAppliedAt(LocalDateTime.of(2026, 5, 19, 9, 0));
        application.setCreatedAt(LocalDateTime.of(2026, 5, 19, 9, 1));
        return application;
    }
}
