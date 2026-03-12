package com.nexus.chatassistant.infrastructure.web;

import com.nexus.chatassistant.application.service.ChatService;
import com.nexus.chatassistant.application.service.UserService;
import com.nexus.chatassistant.domain.exception.ErrorCodes;
import com.nexus.chatassistant.domain.exception.SecurityException;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Adapter for managing server-side rendered chat views.
 */
@Controller
public class ChatWebController {
    private static final Logger log = LoggerFactory.getLogger(ChatWebController.class);
    private final ChatService chatService;
    private final UserService userService;

    public ChatWebController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    /**
     * Loads the chat page. If a sessionId is provided, it loads that session's history.
     */
    @GetMapping({"/", "/chat/{sessionId}"})
    public String chatPage(@PathVariable(required = false) String sessionId,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        log.info("User {} is accessing chat interface. Active session: {}", userDetails.getUsername(), sessionId);

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new SecurityException("User check failed", ErrorCodes.USER_NOT_FOUND));

        List<ChatSession> sessions = chatService.getUserSessions(user.id());
        model.addAttribute("sessions", sessions);
        model.addAttribute("user", user);

        if (sessionId != null) {
            ChatSession session = chatService.getSession(sessionId);
            if (session != null && session.userId().equals(user.id())) {
                model.addAttribute("activeSessionId", sessionId);
                model.addAttribute("messages", chatService.getSessionMessages(sessionId));
                log.debug("Loaded {} messages for session {}",
                        chatService.getSessionMessages(sessionId).size(), sessionId);
            } else {
                log.warn("Access denied for user {} to session {}", user.id(), sessionId);
                throw new SecurityException("Unauthorized session access", ErrorCodes.SESSION_ACCESS_DENIED);
            }
        }

        return "chat";
    }
}