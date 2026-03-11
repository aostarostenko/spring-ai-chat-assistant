package com.nexus.chatassistant.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "sessions")
public record ChatSession(
    @Id String id,
    String userId,
    String summary,
    LocalDateTime timestamp
) {
    public ChatSession(String userId, String summary) {
        this(null, userId, summary, LocalDateTime.now());
    }

    public ChatSession withSummary(String newSummary) {
        return new ChatSession(id, userId, newSummary, timestamp);
    }
}
