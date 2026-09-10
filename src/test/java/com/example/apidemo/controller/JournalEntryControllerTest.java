package com.example.apidemo.controller;

import com.example.apidemo.entity.JournalEntry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class JournalEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createJournalEntryReturnsCreated() throws Exception {
        mockMvc.perform(post("/auth/dev/create-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "journal-flow@example.com",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/journal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "My first entry",
                                  "content": "Hello world"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My first entry"))
                .andExpect(jsonPath("$.content").value("Hello world"));
    }
}
