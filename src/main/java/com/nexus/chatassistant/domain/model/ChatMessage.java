package com.nexus.chatassistant.domain.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "messages")
public record ChatMessage(
    @Id String id,
    String sessionId,
    String sender, // "user" or "ai"
    String content,
    LocalDateTime timestamp
) {
    public ChatMessage(String sessionId, String sender, String content) {
        this(null, sessionId, sender, content, LocalDateTime.now());
    }
}
