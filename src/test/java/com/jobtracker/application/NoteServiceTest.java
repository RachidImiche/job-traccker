package com.jobtracker.application;

import com.jobtracker.application.dto.CreateNoteRequest;
import com.jobtracker.application.dto.NoteResponse;
import com.jobtracker.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @Mock
    private NoteRepository noteRepository;

    private NoteService noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(jobApplicationRepository, noteRepository);
    }

    @Test
    void addNote_savesNoteAndReturnsResponse() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 19, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 19, 10, 0);

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId))
                .thenReturn(Optional.of(application(applicationId, userId)));

        Note saved = new Note();
        ReflectionTestUtils.setField(saved, "id", noteId);
        ReflectionTestUtils.setField(saved, "applicationId", applicationId);
        ReflectionTestUtils.setField(saved, "content", "First follow-up done.");
        ReflectionTestUtils.setField(saved, "createdAt", createdAt);
        ReflectionTestUtils.setField(saved, "updatedAt", updatedAt);
        when(noteRepository.save(any(Note.class))).thenReturn(saved);

        NoteResponse response = noteService.addNote(userId, applicationId, new CreateNoteRequest("  First follow-up done.  "));

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        Note toSave = noteCaptor.getValue();
        assertEquals(applicationId, ReflectionTestUtils.getField(toSave, "applicationId"));
        assertEquals("First follow-up done.", ReflectionTestUtils.getField(toSave, "content"));

        assertEquals(noteId, response.id());
        assertEquals("First follow-up done.", response.content());
        assertEquals(createdAt, response.createdAt());
        assertEquals(updatedAt, response.updatedAt());
    }

    @Test
    void addNote_throwsNotFoundWhenApplicationIsNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> noteService.addNote(userId, applicationId, new CreateNoteRequest("note")));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Application not found", exception.getMessage());
        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
        verifyNoInteractions(noteRepository);
    }

    @Test
    void getNotes_returnsMappedNotesForOwnedApplication() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId))
                .thenReturn(Optional.of(application(applicationId, userId)));

        Note first = note(UUID.randomUUID(), applicationId, "Reached recruiter", LocalDateTime.of(2026, 5, 19, 9, 0));
        Note second = note(UUID.randomUUID(), applicationId, "Referral submitted", LocalDateTime.of(2026, 5, 20, 9, 0));
        when(noteRepository.findAllByApplicationIdOrderByCreatedAtAsc(applicationId)).thenReturn(List.of(first, second));

        List<NoteResponse> response = noteService.getNotes(userId, applicationId);

        assertEquals(2, response.size());
        assertEquals("Reached recruiter", response.getFirst().content());
        assertEquals("Referral submitted", response.get(1).content());
        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
        verify(noteRepository).findAllByApplicationIdOrderByCreatedAtAsc(applicationId);
    }

    @Test
    void getNotes_throwsNotFoundWhenApplicationIsNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> noteService.getNotes(userId, applicationId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Application not found", exception.getMessage());
        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
        verifyNoInteractions(noteRepository);
    }

    @Test
    void deleteNote_deletesNoteWhenOwned() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId))
                .thenReturn(Optional.of(application(applicationId, userId)));

        Note existing = note(noteId, applicationId, "Interview prep done", LocalDateTime.of(2026, 5, 21, 9, 0));
        when(noteRepository.findByIdAndApplicationId(noteId, applicationId)).thenReturn(Optional.of(existing));

        noteService.deleteNote(userId, applicationId, noteId);

        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
        verify(noteRepository).findByIdAndApplicationId(noteId, applicationId);
        verify(noteRepository).delete(existing);
    }

    @Test
    void deleteNote_throwsNotFoundWhenApplicationIsNotOwned() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> noteService.deleteNote(userId, applicationId, noteId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Application not found", exception.getMessage());
        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
        verifyNoInteractions(noteRepository);
    }

    @Test
    void deleteNote_throwsNotFoundWhenNoteDoesNotExistInApplication() {
        UUID userId = UUID.randomUUID();
        UUID applicationId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        when(jobApplicationRepository.findByIdAndUserId(applicationId, userId))
                .thenReturn(Optional.of(application(applicationId, userId)));
        when(noteRepository.findByIdAndApplicationId(noteId, applicationId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> noteService.deleteNote(userId, applicationId, noteId));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Note not found", exception.getMessage());
        verify(jobApplicationRepository).findByIdAndUserId(applicationId, userId);
        verify(noteRepository).findByIdAndApplicationId(noteId, applicationId);
    }

    private JobApplication application(UUID applicationId, UUID userId) {
        JobApplication application = new JobApplication();
        ReflectionTestUtils.setField(application, "id", applicationId);
        ReflectionTestUtils.setField(application, "userId", userId);
        return application;
    }

    private Note note(UUID id, UUID applicationId, String content, LocalDateTime createdAt) {
        Note note = new Note();
        ReflectionTestUtils.setField(note, "id", id);
        ReflectionTestUtils.setField(note, "applicationId", applicationId);
        ReflectionTestUtils.setField(note, "content", content);
        ReflectionTestUtils.setField(note, "createdAt", createdAt);
        ReflectionTestUtils.setField(note, "updatedAt", createdAt.plusMinutes(1));
        return note;
    }
}
