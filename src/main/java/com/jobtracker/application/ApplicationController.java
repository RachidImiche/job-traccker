package com.jobtracker.application;

import com.jobtracker.application.dto.ApplicationResponse;
import com.jobtracker.application.dto.CreateApplicationRequest;
import com.jobtracker.application.dto.UpdateApplicationRequest;
import com.jobtracker.auth.api.AuthenticatedUser;
import com.jobtracker.shared.exception.AppException;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<ApplicationResponse> create(@Valid @RequestBody CreateApplicationRequest request) {
        ApplicationResponse response = applicationService.create(currentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ApplicationResponse>> getAll(@PageableDefault(size = 20) Pageable pageable) {
        Page<ApplicationResponse> response = applicationService.getAll(currentUserId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getById(@PathVariable UUID id) {
        ApplicationResponse response = applicationService.getById(currentUserId(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApplicationResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody UpdateApplicationRequest request) {
        ApplicationResponse response = applicationService.update(currentUserId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        applicationService.delete(currentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authenticatedUser.id();
    }
}
