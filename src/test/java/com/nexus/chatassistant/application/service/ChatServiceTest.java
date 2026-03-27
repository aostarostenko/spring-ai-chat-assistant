package com.nexus.chatassistant.application.service;

import com.nexus.chatassistant.domain.repository.ChatMessageRepository;
import com.nexus.chatassistant.domain.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository sessionRepository;

    @Mock
    private ChatMessageRepository messageRepository;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private SummarizationService summarizationService;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        chatService = new ChatService(sessionRepository, messageRepository, chatClientBuilder, summarizationService, chatMemory);
    }

    @Test
    @DisplayName("Should pass sessionId to advisor parameters during chat call")
    void shouldPassSessionIdToAdvisor() {
        // Given
        String sessionId = "test-session-123";
        String userMessage = "Hello AI";

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(org.springframework.ai.chat.client.advisor.api.Advisor[].class))).thenReturn(requestSpec);
        when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("AI response");

        // When
        chatService.chat(sessionId, userMessage);

        // Then
        // Verify sessionId is passed as chat_memory_conversation_id
        verify(requestSpec).advisors(argThat((Consumer<ChatClient.AdvisorSpec> consumer) -> {
            ChatClient.AdvisorSpec spec = mock(ChatClient.AdvisorSpec.class);
            consumer.accept(spec);
            verify(spec).param("chat_memory_conversation_id", sessionId);
            return true;
        }));
    }
}
