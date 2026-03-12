package com.nexus.chatassistant.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Represents a conversation thread between a user and the AI.
 * Includes domain logic to determine when metadata updates are required.
 */
@Document(collection = "sessions")
public record ChatSession(
        @Id String id,
        String userId,
        String summary,
        LocalDateTime timestamp
) {
    /**
     * Standard constructor for creating new sessions[cite: 63].
     */
    public ChatSession(String userId, String summary) {
        this(null, userId, summary, LocalDateTime.now());
    }

    /**
     * Determines if the session has reached a specific message count
     * threshold to trigger an AI-generated summary update.
     */
    public boolean shouldSummarize(long messageCount) {
        return messageCount > 0 && messageCount % 5 == 0;
    }

    /**
     * Returns a new session instance with an updated summary string[cite: 64].
     */
    public ChatSession withSummary(String newSummary) {
        return new ChatSession(id, userId, newSummary, timestamp);
    }
}