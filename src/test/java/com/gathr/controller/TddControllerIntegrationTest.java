package com.gathr.controller;

import com.gathr.BaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TddControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    @WithMockUser
    void helloWorld_ShouldReturnGreeting() throws Exception {
        mockMvc.perform(get("/api/tdd/hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello TDD!"));
    }
}
