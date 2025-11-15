package com.gathr.service;

import com.gathr.dto.CreateMessageRequest;
import com.gathr.dto.MessageDto;
import com.gathr.entity.Activity;
import com.gathr.entity.Message;
import com.gathr.entity.User;
import com.gathr.repository.ActivityRepository;
import com.gathr.repository.MessageRepository;
import com.gathr.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageService {
    
    private final MessageRepository messageRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    
    public MessageService(MessageRepository messageRepository,
                         ActivityRepository activityRepository,
                         UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
    }
    
    @Transactional(readOnly = true)
    public List<MessageDto> getMessagesByActivity(Long activityId) {
        List<Message> messages = messageRepository.findByActivityIdOrderByCreatedAtAsc(activityId);
        return messages.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public MessageDto createMessage(Long activityId, Long userId, CreateMessageRequest request) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new com.gathr.exception.ResourceNotFoundException("Activity", activityId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.gathr.exception.ResourceNotFoundException("User", userId));
        
        Message message = new Message();
        message.setActivity(activity);
        message.setUser(user);
        message.setText(request.getText());
        
        message = messageRepository.save(message);
        return convertToDto(message);
    }
    
    private MessageDto convertToDto(Message message) {
        return new MessageDto(
            message.getId(),
            message.getActivity().getId(),
            message.getUser().getId(),
            message.getUser().getName(),
            message.getText(),
            message.getCreatedAt()
        );
    }
}

