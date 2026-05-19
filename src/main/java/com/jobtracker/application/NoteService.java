package com.jobtracker.application;

import com.jobtracker.application.dto.CreateNoteRequest;
import com.jobtracker.application.dto.NoteResponse;
import com.jobtracker.shared.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NoteService {

    private final JobApplicationRepository jobApplicationRepository;
    private final NoteRepository noteRepository;

    public NoteService(JobApplicationRepository jobApplicationRepository, NoteRepository noteRepository) {
        this.jobApplicationRepository = jobApplicationRepository;
        this.noteRepository = noteRepository;
    }

    @Transactional
    public NoteResponse addNote(UUID userId, UUID applicationId, CreateNoteRequest request) {
        findOwnedApplication(userId, applicationId);

        Note note = new Note();
        note.setApplicationId(applicationId);
        note.setContent(request.content().trim());

        Note saved = noteRepository.save(note);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> getNotes(UUID userId, UUID applicationId) {
        findOwnedApplication(userId, applicationId);

        return noteRepository.findAllByApplicationIdOrderByCreatedAtAsc(applicationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteNote(UUID userId, UUID applicationId, UUID noteId) {
        findOwnedApplication(userId, applicationId);

        Note note = noteRepository.findByIdAndApplicationId(noteId, applicationId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Note not found"));

        noteRepository.delete(note);
    }

    private void findOwnedApplication(UUID userId, UUID applicationId) {
        jobApplicationRepository.findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Application not found"));
    }

    private NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getContent(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
