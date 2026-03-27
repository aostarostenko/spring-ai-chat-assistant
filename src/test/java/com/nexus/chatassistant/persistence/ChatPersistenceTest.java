package com.nexus.chatassistant.persistence;

import com.nexus.chatassistant.infrastructure.config.AIConfig;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ChatPersistenceTest {

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private MongoTemplate mongoTemplate;

    @AfterEach
    void cleanup() {
        // Task: Cleanup chat_memory collection
        // Note: Spring AI 1.1.2 uses "ai_chat_memory" by default
        mongoTemplate.dropCollection("ai_chat_memory");
    }

    @Test
    void testMessagePersistence() {
        String sessionId = "test-session-" + UUID.randomUUID();
        String secretPhrase = "Secret Phrase: Alpha-Omega-" + UUID.randomUUID();

        // 1. Manually add a message to memory
        var userMessage = new UserMessage("Hello, keep this in Mongo! " + secretPhrase);
        chatMemory.add(sessionId, List.of(userMessage));

        // 2. Verify it exists in the MongoDB collection directly
        // Task: Ensure the test queries the "ai_chat_memory" collection (as defined by default in AIConfig.java)
        var results = mongoTemplate.findAll(Document.class, "ai_chat_memory");

        // Task: Robust Content Check - check raw JSON
        boolean found = results.stream()
                .map(Document::toJson)
                .anyMatch(json -> json.contains(secretPhrase));

        assertTrue(found, "The secret phrase should be found in the raw JSON of the MongoDB document");

        // 3. Verify it's retrievable via the Spring AI API
        List<Message> retrieved = chatMemory.get(sessionId);
        assertFalse(retrieved.isEmpty(), "Should be able to retrieve the message via ChatMemory API");
        assertTrue(retrieved.get(0).getText().contains(secretPhrase), "Retrieved message content should contain the secret phrase");
    }

    @Configuration
    @Import(AIConfig.class)
    static class TestConfig {

        private final List<org.springframework.ai.chat.memory.repository.mongo.Conversation> mockDb = new ArrayList<>();

        @Bean
        public MongoTemplate mongoTemplate() {
            MongoTemplate mock = mock(MongoTemplate.class);

            // Mock insert(Collection, Class) used by MongoChatMemoryRepository.saveAll
            when(mock.insert(anyCollection(), any(Class.class))).thenAnswer(inv -> {
                java.util.Collection<?> items = inv.getArgument(0);
                mockDb.addAll((java.util.Collection<org.springframework.ai.chat.memory.repository.mongo.Conversation>) items);
                return items;
            });

            // Mock findAll(Class, String) used by test for direct check
            when(mock.findAll(eq(Document.class), eq("ai_chat_memory"))).thenAnswer(inv -> {
                return mockDb.stream().map(conv -> {
                    Document doc = new Document();
                    doc.append("conversationId", conv.conversationId());
                    Document msgDoc = new Document();
                    msgDoc.append("content", conv.message().content());
                    msgDoc.append("type", conv.message().type());
                    doc.append("message", msgDoc);
                    return doc;
                }).toList();
            });

            // Mock query(Class) used by MongoChatMemoryRepository.findByConversationId
            org.springframework.data.mongodb.core.ExecutableFindOperation.ExecutableFind mockFind = mock(org.springframework.data.mongodb.core.ExecutableFindOperation.ExecutableFind.class);
            org.springframework.data.mongodb.core.ExecutableFindOperation.TerminatingFind mockTerm = mock(org.springframework.data.mongodb.core.ExecutableFindOperation.TerminatingFind.class);

            when(mock.query(any(Class.class))).thenReturn(mockFind);
            when(mockFind.matching(any(org.springframework.data.mongodb.core.query.Query.class))).thenReturn(mockTerm);
            when(mockTerm.all()).thenAnswer(inv -> new ArrayList<>(mockDb));
            when(mockTerm.stream()).thenAnswer(inv -> mockDb.stream());

            // Mock find(Query, Class, String) if used elsewhere
            when(mock.find(any(), any(Class.class), anyString()))
                    .thenAnswer(inv -> new ArrayList<>(mockDb));

            // Mock converter to avoid NPE in some MongoTemplate internals
            org.springframework.data.mongodb.core.convert.MongoConverter converter = mock(org.springframework.data.mongodb.core.convert.MongoConverter.class);
            when(mock.getConverter()).thenReturn(converter);

            // Mock dropCollection for cleanup
            doAnswer(inv -> {
                mockDb.clear();
                return null;
            }).when(mock).dropCollection(anyString());

            return mock;
        }
    }
}
