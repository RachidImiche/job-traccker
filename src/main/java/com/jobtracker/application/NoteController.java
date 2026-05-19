package com.jobtracker.application;

import com.jobtracker.application.dto.CreateNoteRequest;
import com.jobtracker.application.dto.NoteResponse;
import com.jobtracker.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/applications/{id}/notes")
@Tag(name = "Application Notes", description = "Manage notes for each job application")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    @PostMapping
    @Operation(summary = "Add a note to an application")
    public ResponseEntity<NoteResponse> addNote(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                @PathVariable("id") UUID applicationId,
                                                @Valid @RequestBody CreateNoteRequest request) {
        NoteResponse response = noteService.addNote(authenticatedUser.id(), applicationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List notes for an application")
    public ResponseEntity<List<NoteResponse>> getNotes(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                                       @PathVariable("id") UUID applicationId) {
        List<NoteResponse> response = noteService.getNotes(authenticatedUser.id(), applicationId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{noteId}")
    @Operation(summary = "Delete a note from an application")
    public ResponseEntity<Void> deleteNote(@AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                           @PathVariable("id") UUID applicationId,
                                           @PathVariable UUID noteId) {
        noteService.deleteNote(authenticatedUser.id(), applicationId, noteId);
        return ResponseEntity.noContent().build();
    }
}
