package com.nexus.chatassistant.application.service;

import com.nexus.chatassistant.domain.model.ChatMessage;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.infrastructure.persistence.ChatMessageRepository;
import com.nexus.chatassistant.infrastructure.persistence.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;

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
    }

    public ChatSession startNewSession(String userId) {
        log.info("Creating new chat session for user: {}", userId);
        ChatSession session = new ChatSession(userId, "New Chat");
        return sessionRepository.save(session);
    }

    public List<ChatSession> getUserSessions(String userId) {
        return sessionRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    public List<ChatMessage> getSessionMessages(String sessionId) {
        return messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    public ChatMessage addMessage(String sessionId, String sender, String content) {
        log.debug("Adding message to session {}: {}", sessionId, sender);
        ChatMessage message = new ChatMessage(sessionId, sender, content);
        ChatMessage saved = messageRepository.save(message);

        // Check for summarization trigger
        long count = messageRepository.countBySessionId(sessionId);
        if (count == 5) {
            summarizationService.summarizeAsync(sessionId);
        }

        return saved;
    }

    public String chat(String sessionId, String userMessage) {
        log.info("Processing chat for session: {}", sessionId);

        // 1. Save User message to DB
        addMessage(sessionId, "user", userMessage);

        // 2. Call Gemini (via ChatClient)
        String aiResponse = chatClient.prompt()
                .user(userMessage)
                .call()
                .content();

        // 3. Save AI response to DB
        addMessage(sessionId, "ai", aiResponse);

        // 4. Return the response
        return aiResponse;
    }
}
