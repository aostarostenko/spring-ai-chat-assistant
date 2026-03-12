package com.nexus.chatassistant.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Represents a single message within a chat session.
 * Managed as a MongoDB document in the 'messages' collection.
 */
@Document(collection = "messages")
public record ChatMessage(
        @Id String id,
        String sessionId,
        String sender, // Identifies as "user" or "ai"
        String content,
        LocalDateTime timestamp
) {
    /**
     * Minimal constructor for new messages, defaulting to current time.
     */
    public ChatMessage(String sessionId, String sender, String content) {
        this(null, sessionId, sender, content, LocalDateTime.now());
    }
}