package com.gathr.service;

import com.gathr.entity.Message;
import com.gathr.repository.MessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageExpiryJob {

    private static final Logger logger = LoggerFactory.getLogger(MessageExpiryJob.class);

    private final MessageRepository messageRepository;

    public MessageExpiryJob(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Runs every 15 minutes to delete messages that are older than 24 hours after activity end time
     */
    @Scheduled(fixedRate = 900000) // 15 minutes in milliseconds
    @Transactional
    public void deleteExpiredMessages() {
        logger.info("Starting message expiry job");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusHours(24);
        
        // Find messages that need to be deleted
        // Messages where created_at < (activity.end_time + 24h)
        // This translates to: created_at < (now - 24h) AND activity.end_time < (now - 24h)
        // Actually, we need: created_at < (activity.end_time + 24h)
        // So: created_at < activity.end_time + 24h AND activity.end_time < now - 24h
        
        List<Message> expiredMessages = messageRepository.findExpiredMessages(threshold);
        
        if (!expiredMessages.isEmpty()) {
            logger.info("Deleting {} expired messages", expiredMessages.size());
            messageRepository.deleteAll(expiredMessages);
            logger.info("Successfully deleted {} expired messages", expiredMessages.size());
        } else {
            logger.debug("No expired messages to delete");
        }
    }
}

