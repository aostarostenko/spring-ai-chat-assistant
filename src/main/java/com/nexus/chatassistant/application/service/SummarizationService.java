package com.nexus.chatassistant.application.service;

import com.nexus.chatassistant.domain.model.ChatMessage;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.infrastructure.persistence.ChatMessageRepository;
import com.nexus.chatassistant.infrastructure.persistence.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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

    @Async
    public void summarizeAsync(String sessionId) {
        log.info("Starting background summarization for session: {}", sessionId);
        
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        String conversation = messages.stream()
                .map(m -> m.sender() + ": " + m.content())
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are a helpful assistant. Summarize this conversation in 6 words for a sidebar title.
                Conversation:
                %s
                """.formatted(conversation);

        try {
            String summary = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();
            
            log.info("Generated summary for session {}: {}", sessionId, summary);
            
            sessionRepository.findById(sessionId).ifPresent(session -> {
                ChatSession updated = session.withSummary(summary);
                sessionRepository.save(updated);
            });
        } catch (Exception e) {
            log.error("Failed to summarize session {}: {}", sessionId, e.getMessage());
        }
    }
}
