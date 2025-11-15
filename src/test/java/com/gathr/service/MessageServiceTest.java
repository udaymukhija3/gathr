package com.gathr.service;

import com.gathr.dto.CreateMessageRequest;
import com.gathr.dto.MessageDto;
import com.gathr.entity.Activity;
import com.gathr.entity.Hub;
import com.gathr.entity.Message;
import com.gathr.entity.User;
import com.gathr.exception.ResourceNotFoundException;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.MessageRepository;
import com.gathr.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private MessageService messageService;

    private Activity testActivity;
    private User testUser;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        Hub testHub = new Hub();
        testHub.setId(1L);
        testHub.setName("Cyberhub");

        testUser = new User();
        testUser.setId(1L);
        testUser.setPhone("1234567890");
        testUser.setName("Test User");
        testUser.setVerified(true);

        testActivity = new Activity();
        testActivity.setId(1L);
        testActivity.setTitle("Test Activity");
        testActivity.setHub(testHub);
        testActivity.setCategory(Activity.ActivityCategory.SPORTS);
        testActivity.setStartTime(LocalDateTime.now().plusHours(2));
        testActivity.setEndTime(LocalDateTime.now().plusHours(4));
        testActivity.setCreatedBy(testUser);

        testMessage = new Message();
        testMessage.setId(1L);
        testMessage.setActivity(testActivity);
        testMessage.setUser(testUser);
        testMessage.setText("Hello everyone!");
        testMessage.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void getMessagesByActivity_ShouldReturnListOfMessageDtos() {
        // Given
        Long activityId = 1L;
        List<Message> messages = Arrays.asList(testMessage);

        when(messageRepository.findByActivityIdOrderByCreatedAtAsc(activityId)).thenReturn(messages);

        // When
        List<MessageDto> result = messageService.getMessagesByActivity(activityId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("Hello everyone!");
        assertThat(result.get(0).getUserName()).isEqualTo("Test User");
        assertThat(result.get(0).getActivityId()).isEqualTo(1L);

        verify(messageRepository).findByActivityIdOrderByCreatedAtAsc(activityId);
    }

    @Test
    void getMessagesByActivity_WithNoMessages_ShouldReturnEmptyList() {
        // Given
        Long activityId = 1L;

        when(messageRepository.findByActivityIdOrderByCreatedAtAsc(activityId)).thenReturn(Arrays.asList());

        // When
        List<MessageDto> result = messageService.getMessagesByActivity(activityId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(messageRepository).findByActivityIdOrderByCreatedAtAsc(activityId);
    }

    @Test
    void createMessage_WithValidData_ShouldReturnMessageDto() {
        // Given
        Long activityId = 1L;
        Long userId = 1L;
        CreateMessageRequest request = new CreateMessageRequest();
        request.setText("New message");

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // When
        MessageDto result = messageService.createMessage(activityId, userId, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getActivityId()).isEqualTo(1L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getUserName()).isEqualTo("Test User");

        verify(activityRepository).findById(activityId);
        verify(userRepository).findById(userId);
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void createMessage_WithInvalidActivity_ShouldThrowException() {
        // Given
        Long activityId = 999L;
        Long userId = 1L;
        CreateMessageRequest request = new CreateMessageRequest();
        request.setText("New message");

        when(activityRepository.findById(activityId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> messageService.createMessage(activityId, userId, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Activity not found");

        verify(activityRepository).findById(activityId);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void createMessage_WithInvalidUser_ShouldThrowException() {
        // Given
        Long activityId = 1L;
        Long userId = 999L;
        CreateMessageRequest request = new CreateMessageRequest();
        request.setText("New message");

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> messageService.createMessage(activityId, userId, request))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("User not found");

        verify(userRepository).findById(userId);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void createMessage_ShouldSetCorrectAttributes() {
        // Given
        Long activityId = 1L;
        Long userId = 1L;
        CreateMessageRequest request = new CreateMessageRequest();
        request.setText("Test message content");

        when(activityRepository.findById(activityId)).thenReturn(Optional.of(testActivity));
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            assertThat(message.getActivity()).isEqualTo(testActivity);
            assertThat(message.getUser()).isEqualTo(testUser);
            assertThat(message.getText()).isEqualTo("Test message content");
            return testMessage;
        });

        // When
        messageService.createMessage(activityId, userId, request);

        // Then
        verify(messageRepository).save(any(Message.class));
    }
}
