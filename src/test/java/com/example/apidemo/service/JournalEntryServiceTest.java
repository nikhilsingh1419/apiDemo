package com.example.apidemo.service;

import com.example.apidemo.entity.JournalEntry;
import com.example.apidemo.entity.User;
import com.example.apidemo.exception.ApiException;
import com.example.apidemo.repository.JournalEntryRepository;
import com.example.apidemo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JournalEntryServiceTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private UserRepository userRepository;

    private JournalEntryService journalEntryService;

    @BeforeEach
    void setUp() {
        journalEntryService = new JournalEntryService(journalEntryRepository, userRepository);
    }

    @Test
    void createAssociatesEntryWithUser() {
        User user = new User();
        user.setId(5L);

        JournalEntry request = new JournalEntry();
        request.setTitle("Morning thoughts");
        request.setContent("Hello");

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        JournalEntry saved = journalEntryService.create(5L, request);

        assertEquals(10L, saved.getId());
        assertEquals(user, saved.getUser());
        assertEquals("Morning thoughts", saved.getTitle());
    }

    @Test
    void getByIdForUserReturnsNotFoundWhenMissing() {
        when(journalEntryRepository.findByIdAndUserId(99L, 5L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> journalEntryService.getByIdForUser(99L, 5L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
