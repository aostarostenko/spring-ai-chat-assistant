package com.nexus.chatassistant.infrastructure.web;

import com.nexus.chatassistant.application.service.ChatService;
import com.nexus.chatassistant.application.service.UserService;
import com.nexus.chatassistant.domain.exception.ErrorCodes;
import com.nexus.chatassistant.domain.exception.SecurityException;
import com.nexus.chatassistant.domain.model.ChatMessage;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * WebSocket Adapter responsible for receiving chat intents and broadcasting
 * AI-generated responses back to the client.
 */
@Controller
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, UserService userService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.userService = userService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Receives messages from the client, saves them, fetches an AI response,
     * and broadcasts both updates back to the subscriber.
     */
    @MessageMapping("/chat")
    public void handleMessage(@Payload ChatMessageRequest request, Principal principal) {
        if (principal == null) {
            log.warn("Anonymous user attempted to post to session {}", request.sessionId());
            throw new SecurityException("Anonymous user attempted to post", ErrorCodes.UNAUTHORIZED);
        }

        User user = userService.findByUsername(principal.getName())
                .orElseThrow(() -> new SecurityException("User check failed", ErrorCodes.USER_NOT_FOUND));

        ChatSession session = chatService.getSession(request.sessionId());
        if (session == null || !session.userId().equals(user.id())) {
            log.warn("Access denied for user {} to session {}", user.username(), request.sessionId());
            throw new SecurityException("Unauthorized session access", ErrorCodes.SESSION_ACCESS_DENIED);
        }

        log.info("WebSocket: User '{}' sending content to session {}", user.username(), request.sessionId());

        // 1. Process User Message: Persist and notify UI immediately
        ChatMessage userMsg = chatService.addMessage(request.sessionId(), "user", request.content());
        messagingTemplate.convertAndSend("/topic/messages/" + request.sessionId(), userMsg);
        log.debug("Broadcasted user message for session: {}", request.sessionId());

        // 2. Fetch AI response and notify UI
        String responseText = chatService.chat(request.sessionId(), request.content());
        ChatMessage aiMsg = new ChatMessage(request.sessionId(), "ai", responseText);
        messagingTemplate.convertAndSend("/topic/messages/" + request.sessionId(), aiMsg);
        log.info("Broadcasted AI response for session: {}", request.sessionId());
    }

    public record ChatMessageRequest(String sessionId, String content) {
    }

    /**
     * Intercepts errors occurring during WebSocket message processing.
     * Sends an error message specifically to the user who sent the prompt.
     */
    @MessageExceptionHandler
    @SendToUser("/topic/errors")
    public String handleException(Throwable exception) {
        log.error("WebSocket Error: {}", exception.getMessage());
        return "AI is currently unavailable. Please try again in a moment.";
    }
}