package com.gathr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gathr.dto.CreateActivityRequest;
import com.gathr.entity.Activity;
import com.gathr.entity.Hub;
import com.gathr.entity.User;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.HubRepository;
import com.gathr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ActivityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private HubRepository hubRepository;

    @Autowired
    private UserRepository userRepository;

    private Hub testHub;
    private User testUser;

    @BeforeEach
    void setUp() {
        activityRepository.deleteAll();
        hubRepository.deleteAll();
        userRepository.deleteAll();

        testHub = new Hub();
        testHub.setName("Test Hub");
        testHub.setArea("Test Area");
        testHub = hubRepository.save(testHub);

        testUser = new User();
        testUser.setName("Test User");
        testUser.setPhone("1234567890");
        testUser.setVerified(true);
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser
    void getActivitiesByHub_ReturnsActivities() throws Exception {
        // Arrange
        Activity activity = new Activity();
        activity.setTitle("Test Activity");
        activity.setHub(testHub);
        activity.setCategory(Activity.ActivityCategory.FOOD);
        activity.setStartTime(LocalDateTime.now().plusHours(2));
        activity.setEndTime(LocalDateTime.now().plusHours(4));
        activity.setCreatedBy(testUser);
        activityRepository.save(activity);

        // Act & Assert
        mockMvc.perform(get("/activities")
                        .param("hubId", testHub.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Test Activity")));
    }

    @Test
    @WithMockUser(username = "1")
    void createActivity_Success() throws Exception {
        // Arrange
        CreateActivityRequest request = new CreateActivityRequest();
        request.setTitle("New Activity");
        request.setHubId(testHub.getId());
        request.setCategory(Activity.ActivityCategory.SPORTS);
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setEndTime(LocalDateTime.now().plusHours(4));

        // Act & Assert
        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("New Activity")))
                .andExpect(jsonPath("$.category", is("SPORTS")));
    }

    @Test
    @WithMockUser(username = "1")
    void joinActivity_Success() throws Exception {
        // Arrange
        Activity activity = new Activity();
        activity.setTitle("Test Activity");
        activity.setHub(testHub);
        activity.setCategory(Activity.ActivityCategory.FOOD);
        activity.setStartTime(LocalDateTime.now().plusHours(2));
        activity.setEndTime(LocalDateTime.now().plusHours(4));
        activity.setCreatedBy(testUser);
        activity = activityRepository.save(activity);

        // Act & Assert
        mockMvc.perform(post("/activities/" + activity.getId() + "/join")
                        .param("status", "INTERESTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Successfully joined")));
    }

    @Test
    @WithMockUser(username = "1")
    void confirmActivity_Success() throws Exception {
        // Arrange
        Activity activity = new Activity();
        activity.setTitle("Test Activity");
        activity.setHub(testHub);
        activity.setCategory(Activity.ActivityCategory.FOOD);
        activity.setStartTime(LocalDateTime.now().plusHours(2));
        activity.setEndTime(LocalDateTime.now().plusHours(4));
        activity.setCreatedBy(testUser);
        activity = activityRepository.save(activity);

        // First join the activity
        mockMvc.perform(post("/activities/" + activity.getId() + "/join")
                .param("status", "INTERESTED"));

        // Act & Assert - Confirm attendance
        mockMvc.perform(post("/activities/" + activity.getId() + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Successfully confirmed")));
    }
}
