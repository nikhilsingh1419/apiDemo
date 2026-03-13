package com.example.apidemo.controller;

import com.example.apidemo.entity.JournalEntry;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/journal")
public class JournalEntryController {
    
    private Map<Long, JournalEntry> journalEntries = new HashMap<>();
    
    // Constructor to initialize default entries
    public JournalEntryController() {
        // Default entry 1
        JournalEntry entry1 = new JournalEntry();
        entry1.setId(1);
        entry1.setTitle("First Journal Entry");
        entry1.setContent("This is the content of the first default journal entry.");
        journalEntries.put(1L, entry1);
        
        // Default entry 2
        JournalEntry entry2 = new JournalEntry();
        entry2.setId(2);
        entry2.setTitle("Second Journal Entry");
        entry2.setContent("This is the content of the second default journal entry.");
        journalEntries.put(2L, entry2);
    }
    
    // GET - Get all journal entries
    @GetMapping
    public ResponseEntity<List<JournalEntry>> getAllEntries() {
        return ResponseEntity.ok(new ArrayList<>(journalEntries.values()));
    }
    
    // GET - Get journal entry by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getEntryById(@PathVariable long id) {
        JournalEntry entry = journalEntries.get(id);
        if (entry != null) {
            return ResponseEntity.ok(entry);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Journal entry with ID " + id + " not found.");
    }
    
    // POST - Create a new journal entry (ID must be provided in request body)
    @PostMapping
    public ResponseEntity<?> createEntry(@RequestBody(required = false) JournalEntry entry) {
        if (entry == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Request body is required with journal entry data.");
        }
        
        if (entry.getId() <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("ID must be provided and greater than 0.");
        }
        
        if (journalEntries.containsKey(entry.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Journal entry with ID " + entry.getId() + " already exists.");
        }
        
        journalEntries.put(entry.getId(), entry);
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }
    
    // PUT - Update an existing journal entry by ID
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEntry(@PathVariable long id, @RequestBody JournalEntry updatedEntry) {
        if (!journalEntries.containsKey(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Journal entry with ID " + id + " not found.");
        }
        
        updatedEntry.setId(id);
        journalEntries.put(id, updatedEntry);
        return ResponseEntity.ok(updatedEntry);
    }
    
    // DELETE - Delete a journal entry by ID
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEntry(@PathVariable long id) {
        if (journalEntries.remove(id) != null) {
            return ResponseEntity.ok("Journal entry with ID " + id + " deleted successfully.");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Journal entry with ID " + id + " not found.");
    }
}