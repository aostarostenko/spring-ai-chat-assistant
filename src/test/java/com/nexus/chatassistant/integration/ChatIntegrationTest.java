package com.nexus.chatassistant.integration;

import com.nexus.chatassistant.application.service.ChatService;
import com.nexus.chatassistant.application.service.SummarizationService;
import com.nexus.chatassistant.domain.model.ChatMessage;
import com.nexus.chatassistant.domain.model.ChatSession;
import com.nexus.chatassistant.domain.model.User;
import com.nexus.chatassistant.domain.repository.ChatMessageRepository;
import com.nexus.chatassistant.domain.repository.ChatSessionRepository;
import com.nexus.chatassistant.domain.repository.UserRepository;
import com.nexus.chatassistant.infrastructure.web.ChatController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private ChatSessionRepository sessionRepository;

    @MockitoBean
    private ChatMessageRepository messageRepository;

    @Autowired
    private ChatService chatService;

    @Autowired
    private SummarizationService summarizationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ChatClient.Builder chatClientBuilder;

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;

    private WebSocketStompClient stompClient;
    private String wsUrl;

    private java.util.Map<String, ChatMessage> messagesDb = new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.Map<String, ChatSession> sessionsDb = new java.util.concurrent.ConcurrentHashMap<>();
    private java.util.Map<String, User> usersDb = new java.util.concurrent.ConcurrentHashMap<>();

    private com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void setup() {
        messagesDb.clear();
        sessionsDb.clear();
        usersDb.clear();

        // Setup Repository Mocks
        when(messageRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            ChatMessage msg = inv.getArgument(0);
            String id = msg.id() == null ? java.util.UUID.randomUUID().toString() : msg.id();
            ChatMessage saved = new ChatMessage(id, msg.sessionId(), msg.sender(), msg.content(), msg.timestamp());
            messagesDb.put(id, saved);
            return saved;
        });
        when(messageRepository.findBySessionIdOrderByTimestampAsc(anyString())).thenAnswer(inv -> {
            String sid = inv.getArgument(0);
            return messagesDb.values().stream().filter(m -> m.sessionId().equals(sid)).sorted(java.util.Comparator.comparing(ChatMessage::timestamp)).toList();
        });
        when(messageRepository.countBySessionId(anyString())).thenAnswer(inv -> {
            String sid = inv.getArgument(0);
            return messagesDb.values().stream().filter(m -> m.sessionId().equals(sid)).count();
        });

        when(sessionRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            ChatSession sess = inv.getArgument(0);
            String id = sess.id() == null ? java.util.UUID.randomUUID().toString() : sess.id();
            ChatSession saved = new ChatSession(id, sess.userId(), sess.summary(), sess.timestamp());
            sessionsDb.put(id, saved);
            return saved;
        });
        when(sessionRepository.findById(anyString())).thenAnswer(inv -> {
            String id = inv.getArgument(0);
            return java.util.Optional.ofNullable(sessionsDb.get(id));
        });

        when(userRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            String id = u.id() == null ? java.util.UUID.randomUUID().toString() : u.id();
            User saved = new User(id, u.username(), u.email(), u.password(), u.roles());
            usersDb.put(id, saved);
            usersDb.put(u.username(), saved);
            return saved;
        });
        when(userRepository.findByUsername(anyString())).thenAnswer(inv -> java.util.Optional.ofNullable(usersDb.get((String)inv.getArgument(0))));

        // Mock ChatClient and inject into services because they build it in constructor
        chatClient = Mockito.mock(ChatClient.class);
        requestSpec = Mockito.mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = Mockito.mock(ChatClient.CallResponseSpec.class);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);

        org.springframework.test.util.ReflectionTestUtils.setField(chatService, "chatClient", chatClient);
        org.springframework.test.util.ReflectionTestUtils.setField(summarizationService, "chatClient", chatClient);

        wsUrl = "ws://localhost:" + port + "/chat-websocket";
        stompClient = new WebSocketStompClient(new SockJsClient(
                List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
    }

    private RestTemplate restTemplate = new RestTemplate();

    private String loginAndGetSessionCookie(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("username", username);
        map.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:" + port + "/login", request, String.class);

        List<String> cookies = response.getHeaders().get("Set-Cookie");
        return cookies != null ? cookies.get(0).split(";")[0] : null;
    }

    @Test
    @DisplayName("Full Chat Flow: WebSocket to AI and persistence")
    void shouldHandleFullChatFlow() throws Exception {
        // Given
        String rawPassword = "password123";
        User user = new User("user1", "user1@test.com", passwordEncoder.encode(rawPassword), Set.of("ROLE_USER"));
        User savedUser = userRepository.save(user);
        ChatSession session = new ChatSession(savedUser.id(), "Initial Summary");
        ChatSession savedSession = sessionRepository.save(session);

        String cookie = loginAndGetSessionCookie(savedUser.username(), rawPassword);
        assertThat(cookie).isNotNull();

        when(responseSpec.content()).thenReturn("AI Response text");

        BlockingQueue<ChatMessage> blockingQueue = new LinkedBlockingQueue<>();
        WebSocketHttpHeaders wsHeaders = new WebSocketHttpHeaders();
        wsHeaders.add("Cookie", cookie);

        StompSession stompSession = stompClient.connectAsync(wsUrl, wsHeaders, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);

        stompSession.subscribe("/topic/messages/" + savedSession.id(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ChatMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                blockingQueue.offer((ChatMessage) payload);
            }
        });

        // When
        ChatController.ChatMessageRequest request = new ChatController.ChatMessageRequest(savedSession.id(), "Hello AI");
        stompSession.send("/app/chat", request);

        // Then
        // 1. Verify User Message broadcasted
        ChatMessage receivedUserMsg = blockingQueue.poll(10, TimeUnit.SECONDS);
        assertThat(receivedUserMsg).isNotNull();
        assertThat(receivedUserMsg.sender()).isEqualTo("user");
        assertThat(receivedUserMsg.content()).isEqualTo("Hello AI");

        // 2. Verify AI Message broadcasted
        ChatMessage receivedAiMsg = blockingQueue.poll(10, TimeUnit.SECONDS);
        assertThat(receivedAiMsg).isNotNull();
        assertThat(receivedAiMsg.sender()).isEqualTo("ai");
        assertThat(receivedAiMsg.content()).isEqualTo("AI Response text");

        // 3. Verify Persistence
        List<ChatMessage> dbMessages = messageRepository.findBySessionIdOrderByTimestampAsc(savedSession.id());
        assertThat(dbMessages).hasSize(2);
        assertThat(dbMessages.get(0).sender()).isEqualTo("user");
        assertThat(dbMessages.get(1).sender()).isEqualTo("ai");
    }

    @Test
    @DisplayName("Summarization Trigger: 5 messages trigger title update")
    void shouldTriggerSummarization() throws Exception {
        // Given
        User user = userRepository.save(new User("user-sum", "sum@test.com", "pass", Set.of("ROLE_USER")));
        ChatSession session = sessionRepository.save(new ChatSession(user.id(), "Old Title"));
        
        when(responseSpec.content()).thenReturn("A brief 6 word summary result");

        // When - Send messages using ChatService directly to reach threshold (5)
        // ChatService.addMessage triggers summarization every 5 messages
        for (int i = 0; i < 4; i++) {
            chatService.addMessage(session.id(), "user", "msg " + i);
        }
        
        // The 5th message should trigger it
        chatService.addMessage(session.id(), "user", "triggering msg");

        // Wait for virtual thread to complete
        // Since it's @Async, we need to poll or wait.
        // In SummarizationService it logs "New summary for ...". 
        // We can check the DB periodically.
        
        long startTime = System.currentTimeMillis();
        String updatedSummary = null;
        while (System.currentTimeMillis() - startTime < 5000) {
            updatedSummary = sessionRepository.findById(session.id()).map(ChatSession::summary).orElse("");
            if ("A brief 6 word summary result".equals(updatedSummary)) {
                break;
            }
            Thread.sleep(100);
        }

        // Then
        assertThat(updatedSummary).isEqualTo("A brief 6 word summary result");
    }

    @Test
    @DisplayName("Security & Context: User A cannot see or post to User B session")
    void shouldEnforceSessionIsolation() throws Exception {
        // Given
        String passA = "passA";
        User userA = userRepository.save(new User("userA", "a@test.com", passwordEncoder.encode(passA), Set.of("ROLE_USER")));
        User userB = userRepository.save(new User("userB", "b@test.com", "passB", Set.of("ROLE_USER")));
        
        ChatSession sessionB = sessionRepository.save(new ChatSession(userB.id(), "User B Session"));
        
        // Login as User A
        String cookieA = loginAndGetSessionCookie(userA.username(), passA);
        assertThat(cookieA).isNotNull();

        WebSocketHttpHeaders wsHeaders = new WebSocketHttpHeaders();
        wsHeaders.add("Cookie", cookieA);

        StompSession stompSession = stompClient.connectAsync(wsUrl, wsHeaders, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);

        // When - User A tries to post to User B's session
        ChatController.ChatMessageRequest request = new ChatController.ChatMessageRequest(sessionB.id(), "I am User A but posting to B");
        stompSession.send("/app/chat", request);

        // Wait a bit
        Thread.sleep(1000);

        // Then - Verify no messages were saved for Session B from this attempt
        List<ChatMessage> dbMessages = messageRepository.findBySessionIdOrderByTimestampAsc(sessionB.id());
        assertThat(dbMessages).isEmpty();
    }
}
