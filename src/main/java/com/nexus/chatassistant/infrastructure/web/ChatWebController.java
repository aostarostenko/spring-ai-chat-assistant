package com.nexus.chatassistant.infrastructure.web;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.nexus.chatassistant.application.service.ChatService;
import com.nexus.chatassistant.application.service.SummarizationService;
import com.nexus.chatassistant.application.service.UserService;
import com.nexus.chatassistant.domain.exception.ErrorCodes;
import com.nexus.chatassistant.domain.exception.SecurityException;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.domain.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Adapter for managing server-side rendered chat views.
 */
@Controller
public class ChatWebController {
    private static final Logger log = LoggerFactory.getLogger(ChatWebController.class);
    private final ChatService chatService;
    private final UserService userService;
    private final SummarizationService summarizationService;

    public ChatWebController(ChatService chatService, UserService userService, SummarizationService summarizationService) {
        this.chatService = chatService;
        this.userService = userService;
        this.summarizationService = summarizationService;
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

        // Retroactive summarization check
        for (ChatSession s : sessions) {
            if (s.summary() == null || s.summary().equals("New Chat")) {
                long messageCount = chatService.getMessageCount(s.id());
                if (messageCount > 0) {
                    log.info("Retroactive summarization triggered for session: {}", s.id());
                    summarizationService.summarizeAsync(s.id());
                }
            }
        }

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

    /**
     * Creates a new chat session and redirects to it.
     */
    @GetMapping("/chat/new")
    public String newChat(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new SecurityException("User check failed", ErrorCodes.USER_NOT_FOUND));

        // Create session with "New Chat" as placeholder summary
        ChatSession newSession = new ChatSession(user.id(), null);
        ChatSession saved = chatService.saveSession(newSession);

        log.info("User {} created a new session: {}", user.username(), saved.id());
        return "redirect:/chat/" + saved.id();
    }

    /**
     * Exports the chat session in Markdown or PDF format.
     */
    @GetMapping("/sessions/{sessionId}/export")
    public ResponseEntity<Resource> exportSession(@PathVariable String sessionId,
                                                  @RequestParam(defaultValue = "md") String format,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        log.info("Exporting session {} in {} format for user {}", sessionId, format, userDetails.getUsername());

        User user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new SecurityException("User check failed", ErrorCodes.USER_NOT_FOUND));

        ChatSession session = chatService.getSession(sessionId);
        if (session == null || !session.userId().equals(user.id())) {
            log.warn("Unauthorized export attempt for session {} by user {}", sessionId, user.id());
            throw new SecurityException("Unauthorized session access", ErrorCodes.SESSION_ACCESS_DENIED);
        }

        List<com.nexus.chatassistant.domain.model.ChatMessage> messages = chatService.getSessionMessages(sessionId);
        String summary = session.summary() != null ? session.summary() : "Chat Export";

        if ("pdf".equalsIgnoreCase(format)) {
            return exportAsPdf(summary, messages);
        } else {
            return exportAsMarkdown(summary, messages);
        }
    }

    private ResponseEntity<Resource> exportAsMarkdown(String summary, List<com.nexus.chatassistant.domain.model.ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(summary).append("\n\n");
        for (com.nexus.chatassistant.domain.model.ChatMessage msg : messages) {
            if ("user".equalsIgnoreCase(msg.sender())) {
                sb.append("### User\n");
            }
            sb.append(msg.content()).append("\n\n");
        }
        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
        ByteArrayResource resource = new ByteArrayResource(data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"chat-export.md\"")
                .contentType(MediaType.TEXT_MARKDOWN)
                .contentLength(data.length)
                .body(resource);
    }

    private ResponseEntity<Resource> exportAsPdf(String summary, List<com.nexus.chatassistant.domain.model.ChatMessage> messages) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            document.add(new Paragraph(summary, titleFont));
            document.add(new Paragraph(" ")); // Spacer

            Font senderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            for (com.nexus.chatassistant.domain.model.ChatMessage msg : messages) {
                String sender = "user".equalsIgnoreCase(msg.sender()) ? "User:" : "AI:";
                document.add(new Paragraph(sender, senderFont));
                document.add(new Paragraph(msg.content(), contentFont));
                document.add(new Paragraph(" ")); // Spacer
            }

            document.close();
            byte[] data = out.toByteArray();
            ByteArrayResource resource = new ByteArrayResource(data);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"chat-export.pdf\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(data.length)
                    .body(resource);
        } catch (Exception e) {
            log.error("Failed to generate PDF", e);
            throw new RuntimeException("Export failed", e);
        }
    }
}