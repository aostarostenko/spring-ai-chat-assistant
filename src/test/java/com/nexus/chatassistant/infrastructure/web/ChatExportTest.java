package com.nexus.chatassistant.infrastructure.web;

import com.nexus.chatassistant.application.service.ChatService;
import com.nexus.chatassistant.application.service.UserService;
import com.nexus.chatassistant.domain.model.ChatMessage;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChatExportTest {

    private MockMvc mockMvc;

    @Mock
    private ChatService chatService;

    @Mock
    private UserService userService;

    @Mock
    private com.nexus.chatassistant.application.service.SummarizationService summarizationService;

    @InjectMocks
    private ChatWebController chatWebController;

    private User testUser;
    private ChatSession testSession;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chatWebController).build();

        testUser = new User("user123", "testuser", "Test User", "test@example.com", "password", Set.of("ROLE_USER"));
        testSession = new ChatSession("session123", "user123", "Test Summary", LocalDateTime.now());

        // When using standaloneSetup, we need to mock the AuthenticationPrincipal if needed, 
        // but since we're using Mockito, we can just call the method directly or use a custom Principal if needed.
        // But for MockMvcBuilders.standaloneSetup, we need to use some more configuration for @AuthenticationPrincipal to work.
        // Actually, for simplicity, I'll just test the logic inside the controller by calling it, 
        // OR better yet, let's try to fix the @WebMvcTest issues.
    }

    @Test
    void testExportMarkdownLogic() throws Exception {
        // Since @AuthenticationPrincipal is hard to mock in standalone, 
        // I'll mock the user and session directly in the logic.
        
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatService.getSession("session123")).thenReturn(testSession);
        when(chatService.getSessionMessages("session123")).thenReturn(List.of(
                new ChatMessage("session123", "user", "Hello AI"),
                new ChatMessage("session123", "ai", "Hello User")
        ));

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetails.getUsername()).thenReturn("testuser");

        // We can't easily use mockMvc with @AuthenticationPrincipal in standalone without extra setup.
        // Let's just call the controller method directly to verify the logic.
        var response = chatWebController.exportSession("session123", "md", userDetails);

        org.junit.jupiter.api.Assertions.assertEquals(200, response.getStatusCode().value());
        org.junit.jupiter.api.Assertions.assertTrue(response.getHeaders().getContentDisposition().isAttachment());
        org.junit.jupiter.api.Assertions.assertEquals("chat-export.md", response.getHeaders().getContentDisposition().getFilename());
    }
}
