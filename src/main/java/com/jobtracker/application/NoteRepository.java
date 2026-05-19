package com.jobtracker.application;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {

    List<Note> findAllByApplicationIdOrderByCreatedAtAsc(UUID applicationId);

    Optional<Note> findByIdAndApplicationId(UUID id, UUID applicationId);
}
