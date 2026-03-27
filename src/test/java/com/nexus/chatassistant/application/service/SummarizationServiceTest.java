package com.nexus.chatassistant.application.service;

import com.nexus.chatassistant.domain.model.ChatMessage;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.domain.repository.ChatMessageRepository;
import com.nexus.chatassistant.domain.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SummarizationServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private SummarizationService summarizationService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        summarizationService = new SummarizationService(sessionRepository, messageRepository, chatClientBuilder, messagingTemplate);
    }

    @Test
    @DisplayName("Should generate a 6-word summary and update the session")
    void shouldGenerateSummaryAndUpdateSession() throws Exception {
        // Given
        String sessionId = "session-123";
        List<ChatMessage> messages = List.of(
                new ChatMessage(sessionId, "user", "Hello"),
                new ChatMessage(sessionId, "ai", "How can I help?")
        );
        ChatSession existingSession = new ChatSession("user-1", "Original Summary");

        when(messageRepository.findBySessionIdOrderByTimestampAsc(sessionId)).thenReturn(messages);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Summarized: Help requested by user.");
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(existingSession));

        // When
        // Using Virtual Threads to execute the task as requested
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            executor.submit(() -> summarizationService.summarizeAsync(sessionId)).get(5, TimeUnit.SECONDS);
        }

        // Then
        verify(messageRepository).findBySessionIdOrderByTimestampAsc(sessionId);
        verify(chatClient.prompt()).user(argThat((String prompt) -> prompt.contains("Summarize the following conversation in exactly 6 words")));
        verify(sessionRepository).save(argThat(session -> session.summary().equals("Summarized: Help requested by user.")));
        verify(messagingTemplate).convertAndSend(eq("/topic/session-update/" + sessionId), argThat((ChatSession session) -> session.summary().equals("Summarized: Help requested by user.")));
    }

    @Test
    @DisplayName("Should handle exceptions and log error during summarization")
    void shouldHandleSummarizationError() {
        // Given
        String sessionId = "session-error";
        when(messageRepository.findBySessionIdOrderByTimestampAsc(sessionId)).thenReturn(List.of());
        when(chatClient.prompt()).thenThrow(new RuntimeException("AI Model Error"));

        // When
        summarizationService.summarizeAsync(sessionId);

        // Then
        verify(sessionRepository, never()).save(any());
    }
}
