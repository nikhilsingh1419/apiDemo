package com.example.apidemo.repository;

import com.example.apidemo.entity.AuthProvider;
import com.example.apidemo.entity.JournalEntry;
import com.example.apidemo.entity.User;
import com.example.apidemo.repository.JournalEntryRepository;
import com.example.apidemo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JournalEntryRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Test
    void savesAndFindsEntriesByUserId() {
        User user = new User();
        user.setEmail("journal-repo-test@example.com");
        user.setName("Journal Repo Test");
        user.setGoogleSub("email:journal-repo-test@example.com");
        user.setAuthProvider(AuthProvider.EMAIL);
        user = userRepository.save(user);

        JournalEntry entry = new JournalEntry();
        entry.setUser(user);
        entry.setTitle("Repo test entry");
        entry.setContent("content");
        entry = journalEntryRepository.save(entry);

        List<JournalEntry> entries = journalEntryRepository.findByUserIdOrderByIdDesc(user.getId());

        assertFalse(entries.isEmpty());
        assertEquals("Repo test entry", entries.get(0).getTitle());
    }
}
