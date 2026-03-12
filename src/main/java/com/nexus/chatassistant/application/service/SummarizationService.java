package com.nexus.chatassistant.application.service;

import com.nexus.chatassistant.domain.model.ChatMessage;
import com.nexus.chatassistant.domain.repository.ChatMessageRepository;
import com.nexus.chatassistant.domain.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
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

    public SummarizationService(ChatSessionRepository sessionRepository,
                                ChatMessageRepository messageRepository,
                                ChatClient.Builder chatClientBuilder) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Asynchronously generates a 6-word summary of the conversation.
     */
    @Async
    public void summarizeAsync(String sessionId) {
        log.info("Starting background summarization for session: {}", sessionId);

        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        String history = messages.stream()
                .map(m -> m.sender() + ": " + m.content())
                .collect(Collectors.joining("\n"));

        String prompt = "Summarize the following conversation in exactly 6 words for a sidebar title: \n" + history;

        try {
            String summary = chatClient.prompt().user(prompt).call().content();
            log.info("New summary for {}: {}", sessionId, summary);

            sessionRepository.findById(sessionId).ifPresent(session -> {
                sessionRepository.save(session.withSummary(summary));
                log.debug("Session {} metadata updated in MongoDB.", sessionId);
            });
        } catch (Exception e) {
            log.error("Failed to summarize session {}: {}", sessionId, e.getMessage());
        }
    }
}