package com.nexus.chatassistant.application.service;

import com.nexus.chatassistant.domain.model.ChatMessage;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.domain.repository.ChatMessageRepository;
import com.nexus.chatassistant.domain.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates chat flows, managing session state and message persistence.
 * Connects the web layer to the Gemini AI model via Spring AI.
 */
@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatClient chatClient;
    private final SummarizationService summarizationService;

    public ChatService(ChatSessionRepository sessionRepository,
                       ChatMessageRepository messageRepository,
                       ChatClient.Builder chatClientBuilder,
                       SummarizationService summarizationService) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.chatClient = chatClientBuilder.build();
        this.summarizationService = summarizationService;
        log.info("ChatService initialized with Gemini 3 capabilities.");
    }

    /**
     * Persists a message and checks if the session reaches the summarization threshold.
     */
    public ChatMessage addMessage(String sessionId, String sender, String content) {
        log.debug("Session {}: Adding {} message.", sessionId, sender);
        ChatMessage message = new ChatMessage(sessionId, sender, content);
        ChatMessage saved = messageRepository.save(message);

        long count = messageRepository.countBySessionId(sessionId);

        // Logic: Summarize every 5 messages to keep the sidebar context relevant
        if (count > 0 && count % 5 == 0) {
            log.info("Summarization threshold (5) reached for session {}. Triggering AI...", sessionId);
            summarizationService.summarizeAsync(sessionId);
        }
        return saved;
    }

    /**
     * Executes a full chat cycle: saves user message, fetches AI response, and saves AI message.
     */
    public String chat(String sessionId, String userMessage) {
        log.info("Processing chat request for session: {}", sessionId);

        addMessage(sessionId, "user", userMessage);

        String aiResponse = chatClient.prompt()
                .user(userMessage)
                .call()
                .content();

        addMessage(sessionId, "ai", aiResponse);

        log.info("Successfully processed AI response for session: {}", sessionId);
        return aiResponse;
    }

    public List<ChatSession> getUserSessions(String userId) {
        return sessionRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public List<ChatMessage> getSessionMessages(String sessionId) {
        return messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }
}