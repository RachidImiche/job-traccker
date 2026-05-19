package com.jobtracker.application;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    Optional<JobApplication> findByIdAndUserId(UUID id, UUID userId);

    Page<JobApplication> findAllByUserId(UUID userId, Pageable pageable);
}
