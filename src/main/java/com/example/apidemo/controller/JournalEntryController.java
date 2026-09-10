package com.example.apidemo.controller;

import com.example.apidemo.entity.JournalEntry;
import com.example.apidemo.exception.ApiException;
import com.example.apidemo.security.AuthenticatedUser;
import com.example.apidemo.security.SecurityUtils;
import com.example.apidemo.service.JournalEntryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/journal")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    public JournalEntryController(JournalEntryService journalEntryService) {
        this.journalEntryService = journalEntryService;
    }

    @GetMapping
    public ResponseEntity<List<JournalEntry>> getAllEntries() {
        AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
        return ResponseEntity.ok(journalEntryService.getAllForUser(currentUser.userId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEntryById(@PathVariable Long id) {
        try {
            AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
            return ResponseEntity.ok(journalEntryService.getByIdForUser(id, currentUser.userId()));
        } catch (ApiException ex) {
            return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createEntry(@RequestBody(required = false) JournalEntry entry) {
        try {
            AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
            JournalEntry saved = journalEntryService.create(currentUser.userId(), entry);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.BAD_REQUEST) {
                return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
            }
            throw ex;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEntry(@PathVariable Long id, @RequestBody JournalEntry updatedEntry) {
        try {
            AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
            return ResponseEntity.ok(journalEntryService.update(id, currentUser.userId(), updatedEntry));
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.NOT_FOUND || ex.getStatus() == HttpStatus.BAD_REQUEST) {
                return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
            }
            throw ex;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEntry(@PathVariable Long id) {
        try {
            AuthenticatedUser currentUser = SecurityUtils.requireCurrentUser();
            journalEntryService.delete(id, currentUser.userId());
            return ResponseEntity.ok("Journal entry with ID " + id + " deleted successfully.");
        } catch (ApiException ex) {
            return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
        }
    }
}
