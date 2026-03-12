package com.nexus.chatassistant.domain.repository;

import com.nexus.chatassistant.domain.model.ChatSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Port for accessing chat sessions stored in MongoDB.
 */
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    /**
     * Finds all sessions for a user, sorted by most recent first.
     */
    List<ChatSession> findByUserIdOrderByTimestampDesc(String userId);
}