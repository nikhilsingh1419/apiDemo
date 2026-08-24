package com.example.apidemo.controller;

import com.example.apidemo.entity.JournalEntry;
import com.example.apidemo.repository.JournalEntryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/journal")
public class JournalEntryController {

    private final JournalEntryRepository journalEntryRepository;

    public JournalEntryController(JournalEntryRepository journalEntryRepository) {
        this.journalEntryRepository = journalEntryRepository;
    }

    @GetMapping
    public ResponseEntity<List<JournalEntry>> getAllEntries() {
        return ResponseEntity.ok(journalEntryRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getEntryById(@PathVariable Long id) {
        return journalEntryRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Journal entry with ID " + id + " not found."));
    }

    @PostMapping
    public ResponseEntity<?> createEntry(@RequestBody(required = false) JournalEntry entry) {
        if (entry == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Request body is required with journal entry data.");
        }
        if (entry.getTitle() == null || entry.getTitle().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Title is required.");
        }
        entry.setId(null);
        JournalEntry saved = journalEntryRepository.save(entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEntry(@PathVariable Long id, @RequestBody JournalEntry updatedEntry) {
        return journalEntryRepository.findById(id)
                .<ResponseEntity<?>>map(existing -> {
                    updatedEntry.setId(id);
                    return ResponseEntity.ok(journalEntryRepository.save(updatedEntry));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Journal entry with ID " + id + " not found."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEntry(@PathVariable Long id) {
        if (journalEntryRepository.existsById(id)) {
            journalEntryRepository.deleteById(id);
            return ResponseEntity.ok("Journal entry with ID " + id + " deleted successfully.");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Journal entry with ID " + id + " not found.");
    }
}
