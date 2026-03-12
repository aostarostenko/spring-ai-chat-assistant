package com.nexus.chatassistant.infrastructure.web;

import com.nexus.chatassistant.application.service.ChatService;
import com.nexus.chatassistant.application.service.UserService;
import com.nexus.chatassistant.domain.exception.SecurityException;
import com.nexus.chatassistant.domain.model.ChatMessage;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private UserService userService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatController chatController;

    @Test
    @DisplayName("Should broadcast user message and AI response through WebSocket")
    void shouldBroadcastMessages() {
        // Given
        String sessionId = "session-456";
        String content = "Hello AI";
        String aiResponse = "Hello Human";
        String username = "testuser";
        String userId = "user-123";
        ChatController.ChatMessageRequest request = new ChatController.ChatMessageRequest(sessionId, content);
        
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(username);

        User user = new User(userId, username, "email", "pass", Set.of("ROLE_USER"));
        ChatSession session = new ChatSession(sessionId, userId, "Title", null);
        
        when(userService.findByUsername(username)).thenReturn(Optional.of(user));
        when(chatService.getSession(sessionId)).thenReturn(session);
        
        ChatMessage userMsg = new ChatMessage(sessionId, "user", content);
        when(chatService.addMessage(sessionId, "user", content)).thenReturn(userMsg);
        when(chatService.chat(sessionId, content)).thenReturn(aiResponse);

        // When
        chatController.handleMessage(request, principal);

        // Then
        verify(messagingTemplate).convertAndSend(
                eq("/topic/messages/" + sessionId),
                argThat((ChatMessage msg) -> msg.sender().equals("user") && msg.content().equals(content))
        );
        verify(messagingTemplate).convertAndSend(
                eq("/topic/messages/" + sessionId),
                argThat((ChatMessage msg) -> msg.sender().equals("ai") && msg.content().equals(aiResponse))
        );
    }

    @Test
    @DisplayName("Should reject message if principal is null")
    void shouldRejectIfPrincipalNull() {
        // Given
        ChatController.ChatMessageRequest request = new ChatController.ChatMessageRequest("sess", "hi");

        // When / Then
        assertThatThrownBy(() -> chatController.handleMessage(request, null))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Anonymous user attempted to post");

        verify(chatService, never()).addMessage(any(), any(), any());
    }

    @Test
    @DisplayName("Should reject message if session does not belong to user")
    void shouldRejectIfSessionNotOwned() {
        // Given
        String sessionId = "other-session";
        String username = "userA";
        ChatController.ChatMessageRequest request = new ChatController.ChatMessageRequest(sessionId, "hi");
        
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(username);
        
        User userA = new User("idA", username, "email", "pass", Set.of("ROLE_USER"));
        ChatSession sessionB = new ChatSession(sessionId, "idB", "Title", null);
        
        when(userService.findByUsername(username)).thenReturn(Optional.of(userA));
        when(chatService.getSession(sessionId)).thenReturn(sessionB);

        // When / Then
        assertThatThrownBy(() -> chatController.handleMessage(request, principal))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Unauthorized session access");

        verify(chatService, never()).addMessage(any(), any(), any());
    }
}
