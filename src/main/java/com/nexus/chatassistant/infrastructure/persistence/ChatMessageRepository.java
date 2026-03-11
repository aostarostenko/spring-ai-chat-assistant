package com.nexus.chatassistant.infrastructure.persistence;

import com.nexus.chatassistant.domain.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
    long countBySessionId(String sessionId);
}
