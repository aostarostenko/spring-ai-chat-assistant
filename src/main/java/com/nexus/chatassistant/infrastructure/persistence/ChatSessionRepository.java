package com.nexus.chatassistant.infrastructure.persistence;

import com.nexus.chatassistant.domain.model.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    List<ChatSession> findByUserIdOrderByTimestampDesc(String userId);
}
