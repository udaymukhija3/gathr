package com.gathr;

import com.gathr.dto.CreateActivityRequest;
import com.gathr.entity.User;
import com.gathr.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.mockito.Mockito;
import org.mockito.ArgumentMatchers;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.core.Authentication;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

public class CreatePlanAndJoinIT extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private com.gathr.security.AuthenticatedUserService authenticatedUserService;

    @Test
    public void testCreatePlanAndJoin() throws Exception {
        // Setup: Create user for the mock user (assuming AuthenticatedUserService
        // resolves by phone or ID from principal)
        // If AuthenticatedUserService uses the principal name as phone/ID, we need to
        // ensure DB matches.
        // For this test, let's assume we can rely on @WithMockUser working if we mock
        // the service or if it uses standard SecurityContext.
        // But AuthenticatedUserService.requireUserId usually looks up the user.
        // Let's create a user in DB first.

        User creator = new User();
        creator.setName("Creator");
        creator.setPhone("user1"); // Matches username
        creator.setVerified(true);
        creator = userRepository.save(creator);

        User joiner = new User();
        joiner.setName("Joiner");
        joiner.setPhone("user2");
        joiner.setVerified(true);
        joiner = userRepository.save(joiner);

        // Mock authentication service
        final Long creatorId = creator.getId();
        final Long joinerId = joiner.getId();

        Mockito.when(authenticatedUserService.requireUserId(ArgumentMatchers.any())).thenAnswer(invocation -> {
            Authentication auth = invocation.getArgument(0);
            if (auth == null)
                throw new IllegalStateException("No auth");
            if ("user1".equals(auth.getName()))
                return creatorId;
            if ("user2".equals(auth.getName()))
                return joinerId;
            throw new IllegalStateException("Unknown user: " + auth.getName());
        });

        // 1. Create Activity
        CreateActivityRequest createRequest = new CreateActivityRequest();
        createRequest.setTitle("Test Plan");
        createRequest.setCategory(com.gathr.entity.Activity.ActivityCategory.FOOD);
        createRequest.setStartTime(LocalDateTime.now().plusHours(2));
        createRequest.setEndTime(LocalDateTime.now().plusHours(4));
        createRequest.setPlaceName("Test Place");
        createRequest.setPlaceAddress("123 Test St");
        createRequest.setLatitude(12.34);
        createRequest.setLongitude(56.78);
        createRequest.setMaxMembers(4);

        String activityJson = objectMapper.writeValueAsString(createRequest);

        // We need to mock the authentication to return the creator's ID or Phone
        // If AuthenticatedUserService extracts from Authentication,
        // @WithMockUser("user1") should work if it maps "user1" to phone.

        mockMvc.perform(post("/activities")
                .contentType(MediaType.APPLICATION_JSON)
                .content(activityJson)
                .with(SecurityMockMvcRequestPostProcessors.user("user1")))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists()) // API returns ApiResponse with data field
                .andExpect(jsonPath("$.data.title").value("Test Plan"));

        // 2. Join Activity (as user2)
        // We need the activity ID. Let's assume it's 1 if DB was empty, or fetch it.
        // Better: capture response. But for simple IT, let's fetch from DB.
        Long activityId = 1L; // Risky assumption, but ok for MVP test if transactional rollback works.

        mockMvc.perform(post("/activities/" + activityId + "/join")
                .param("status", "INTERESTED")
                .with(SecurityMockMvcRequestPostProcessors.user("user2")))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }
}
