package com.nexus.chatassistant.application.service;

import com.nexus.chatassistant.domain.exception.DaoException;
import com.nexus.chatassistant.domain.exception.ErrorCodes;
import com.nexus.chatassistant.domain.model.ChatMessage;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.domain.repository.ChatMessageRepository;
import com.nexus.chatassistant.domain.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.dao.DataAccessException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Background service for conversation summarization.
 * Updates chat session titles to help users navigate history.
 */
@Service
public class SummarizationService {
    private static final Logger log = LoggerFactory.getLogger(SummarizationService.class);
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatClient chatClient;
    private final SimpMessagingTemplate messagingTemplate;

    public SummarizationService(ChatSessionRepository sessionRepository,
                                ChatMessageRepository messageRepository,
                                ChatClient.Builder chatClientBuilder,
                                SimpMessagingTemplate messagingTemplate) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.chatClient = chatClientBuilder.build();
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Asynchronously generates a 6-word summary of the conversation.
     */
    @Async
    public void summarizeAsync(String sessionId) {
        log.info("Starting background summarization for session: {}", sessionId);

        List<ChatMessage> messages;
        try {
            messages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        } catch (DataAccessException e) {
            throw new DaoException("DB Error", ErrorCodes.DB_READ_FAILURE, e);
        }

        String history = messages.stream()
                .map(m -> m.sender() + ": " + m.content())
                .collect(Collectors.joining("\n"));

        String prompt = "Summarize the following conversation in exactly 6 words for a sidebar title: \n" + history;

        try {
            String summary = chatClient.prompt().user(prompt).call().content();
            log.info("New summary for {}: {}", sessionId, summary);

            try {
                sessionRepository.findById(sessionId).ifPresent(session -> {
                    try {
                        ChatSession updatedSession = session.withSummary(summary);
                        sessionRepository.save(updatedSession);
                        log.debug("Session {} metadata updated in MongoDB.", sessionId);

                        // Task 2: Broadcast the updated session object via WebSocket
                        messagingTemplate.convertAndSend("/topic/session-update/" + sessionId, updatedSession);
                        log.info("Broadcasted session update for {}: {}", sessionId, summary);
                    } catch (DataAccessException e) {
                        throw new DaoException("DB Error", ErrorCodes.DB_WRITE_FAILURE, e);
                    }
                });
            } catch (DataAccessException e) {
                throw new DaoException("DB Error", ErrorCodes.DB_READ_FAILURE, e);
            }
        } catch (Exception e) {
            log.error("Failed to summarize session {}: {}", sessionId, e.getMessage());
        }
    }
}