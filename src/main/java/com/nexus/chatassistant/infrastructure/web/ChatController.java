package com.nexus.chatassistant.infrastructure.web;

import com.nexus.chatassistant.application.service.ChatService;
import com.nexus.chatassistant.domain.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat")
    public void handleMessage(@Payload ChatMessageRequest request,
                              Principal principal) {
        String sessionId = request.sessionId();
        log.info("Received WebSocket message for session {}: {}", sessionId, request.content());
        
        // 1. Save and broadcast user message
        // ChatService.chat handles saving both, but we need to broadcast user message first for UI.
        ChatMessage userMsg = new ChatMessage(sessionId, "user", request.content());
        // Note: ChatService.chat will save it again if we are not careful.
        // Actually the requirement for ChatService.chat says "1. Save User message to DB."
        // So I should let ChatService do it.
        
        // Wait, if I want real-time, I should broadcast user message immediately.
        // But if I call chatService.chat(), it will save it first.
        
        // Let's call chatService.chat() and broadcast its results.
        // Actually, to be reactive:
        // - Broadcast user message (it's not saved yet, or we save it then broadcast)
        // - Call Gemini
        // - Save AI response
        // - Broadcast AI response
        
        // The description for chatService.chat is very specific. 
        // I'll call it, but I'll manually broadcast the user message BEFORE calling it 
        // OR I will refactor chatService.chat slightly if possible, but the requirement is strict.
        
        // Let's follow the requirement:
        // 1. Broadcast user message (immediately for UI responsiveness)
        ChatMessage tempUserMsg = new ChatMessage(sessionId, "user", request.content());
        messagingTemplate.convertAndSend("/topic/messages/" + sessionId, tempUserMsg);

        // 2. Call ChatService.chat which saves both messages and returns AI response
        String aiResponse = chatService.chat(sessionId, request.content());
        
        // 3. Broadcast AI response
        ChatMessage aiMsg = new ChatMessage(sessionId, "ai", aiResponse);
        messagingTemplate.convertAndSend("/topic/messages/" + sessionId, aiMsg);
    }

    public record ChatMessageRequest(String sessionId, String content) {}
}
