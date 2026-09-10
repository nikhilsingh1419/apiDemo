package com.example.apidemo.service;

import com.example.apidemo.entity.JournalEntry;
import com.example.apidemo.entity.User;
import com.example.apidemo.exception.ApiException;
import com.example.apidemo.repository.JournalEntryRepository;
import com.example.apidemo.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class JournalEntryService {

    private final JournalEntryRepository journalEntryRepository;
    private final UserRepository userRepository;

    public JournalEntryService(JournalEntryRepository journalEntryRepository, UserRepository userRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> getAll(Long userId) {
        if (userId != null) {
            return journalEntryRepository.findByUserIdOrderByIdDesc(userId);
        }
        return journalEntryRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Transactional(readOnly = true)
    public JournalEntry getById(Long id, Long userId) {
        if (userId != null) {
            return journalEntryRepository.findByIdAndUserId(id, userId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND, "Journal entry with ID " + id + " not found."));
        }
        return journalEntryRepository.findById(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "Journal entry with ID " + id + " not found."));
    }

    @Transactional
    public JournalEntry create(Long userId, JournalEntry entry) {
        if (entry == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Request body is required with journal entry data.");
        }
        if (entry.getTitle() == null || entry.getTitle().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Title is required.");
        }

        User user = resolveUser(userId);

        JournalEntry journalEntry = new JournalEntry();
        journalEntry.setUser(user);
        journalEntry.setTitle(entry.getTitle().trim());
        journalEntry.setContent(entry.getContent());
        return journalEntryRepository.save(journalEntry);
    }

    @Transactional
    public JournalEntry update(Long id, Long userId, JournalEntry updatedEntry) {
        JournalEntry existing = getById(id, userId);
        if (updatedEntry.getTitle() == null || updatedEntry.getTitle().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Title is required.");
        }
        existing.setTitle(updatedEntry.getTitle().trim());
        existing.setContent(updatedEntry.getContent());
        return journalEntryRepository.save(existing);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        if (userId != null) {
            if (!journalEntryRepository.existsByIdAndUserId(id, userId)) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Journal entry with ID " + id + " not found.");
            }
        } else if (!journalEntryRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Journal entry with ID " + id + " not found.");
        }
        journalEntryRepository.deleteById(id);
    }

    private User resolveUser(Long userId) {
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found."));
        }
        return userRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "No users found. Create one with POST /auth/dev/create-user first."));
    }
}
