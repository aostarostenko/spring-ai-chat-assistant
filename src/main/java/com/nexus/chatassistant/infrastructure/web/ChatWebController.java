package com.nexus.chatassistant.infrastructure.web;

import com.nexus.chatassistant.application.service.ChatService;
import com.nexus.chatassistant.application.service.UserService;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.domain.model.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class ChatWebController {
    private final ChatService chatService;
    private final UserService userService;

    public ChatWebController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        return chatPage(null, userDetails, model);
    }

    @GetMapping("/chat/new")
    public String newChat(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        ChatSession session = chatService.startNewSession(user.id());
        return "redirect:/chat/" + session.id();
    }

    @GetMapping("/chat/{sessionId}")
    public String chatPage(@PathVariable(required = false) String sessionId,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<ChatSession> sessions = chatService.getUserSessions(user.id());
        model.addAttribute("sessions", sessions);
        model.addAttribute("user", user);

        if (sessionId != null) {
            model.addAttribute("activeSessionId", sessionId);
            model.addAttribute("messages", chatService.getSessionMessages(sessionId));
        } else if (!sessions.isEmpty()) {
            // Automatically redirect to latest session?
            // return "redirect:/chat/" + sessions.get(0).id();
            // Let's stay on blank page or load latest
        }

        return "chat";
    }
}
