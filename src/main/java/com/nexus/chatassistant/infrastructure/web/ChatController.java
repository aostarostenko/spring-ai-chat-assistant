package com.nexus.chatassistant.infrastructure.web;

import com.nexus.chatassistant.application.service.ChatService;
import com.nexus.chatassistant.domain.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Receives messages from the client, saves them, fetches an AI response,
     * and broadcasts both updates back to the subscriber.
     */
    @MessageMapping("/chat")
    public void handleMessage(@Payload ChatMessageRequest request, Principal principal) {
        String username = (principal != null) ? principal.getName() : "anonymous";
        log.info("WebSocket: User '{}' sending content to session {}", username, request.sessionId());

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
}