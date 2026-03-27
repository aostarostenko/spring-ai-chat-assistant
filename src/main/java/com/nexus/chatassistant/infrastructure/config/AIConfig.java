package com.nexus.chatassistant.infrastructure.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.mongo.MongoChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class AIConfig {

    @Bean
    public ChatMemory chatMemory(MongoTemplate mongoTemplate) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(MongoChatMemoryRepository.builder()
                        .mongoTemplate(mongoTemplate)
                        .build())
                .maxMessages(10) // Equivalent to RETRIEVE_SIZE
                .build();
    }
}
