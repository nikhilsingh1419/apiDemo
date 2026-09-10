package com.example.apidemo.controller;

import com.example.apidemo.entity.JournalEntry;
import com.example.apidemo.exception.ApiException;
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
        Long userId = SecurityUtils.optionalCurrentUser().map(user -> user.userId()).orElse(null);
        return ResponseEntity.ok(journalEntryService.getAll(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEntryById(@PathVariable Long id) {
        try {
            Long userId = SecurityUtils.optionalCurrentUser().map(user -> user.userId()).orElse(null);
            return ResponseEntity.ok(journalEntryService.getById(id, userId));
        } catch (ApiException ex) {
            return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createEntry(@RequestBody(required = false) JournalEntry entry) {
        try {
            Long userId = SecurityUtils.optionalCurrentUser().map(user -> user.userId()).orElse(null);
            JournalEntry saved = journalEntryService.create(userId, entry);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (ApiException ex) {
            return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEntry(@PathVariable Long id, @RequestBody JournalEntry updatedEntry) {
        try {
            Long userId = SecurityUtils.optionalCurrentUser().map(user -> user.userId()).orElse(null);
            return ResponseEntity.ok(journalEntryService.update(id, userId, updatedEntry));
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
            Long userId = SecurityUtils.optionalCurrentUser().map(user -> user.userId()).orElse(null);
            journalEntryService.delete(id, userId);
            return ResponseEntity.ok("Journal entry with ID " + id + " deleted successfully.");
        } catch (ApiException ex) {
            return ResponseEntity.status(ex.getStatus()).body(ex.getMessage());
        }
    }
}
