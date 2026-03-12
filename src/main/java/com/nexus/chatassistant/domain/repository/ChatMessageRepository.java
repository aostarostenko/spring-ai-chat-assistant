package com.nexus.chatassistant.domain.repository;

import com.nexus.chatassistant.domain.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Port for accessing persisted chat messages.
 */
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    /**
     * Retrieves messages for a specific session in chronological order.
     */
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);

    /**
     * Counts the total number of messages in a session for summarization triggers.
     */
    long countBySessionId(String sessionId);
}