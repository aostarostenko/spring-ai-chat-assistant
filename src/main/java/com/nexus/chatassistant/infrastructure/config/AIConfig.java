package com.nexus.chatassistant.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.genai.types.FinishReason;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.mongo.MongoChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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

    @Bean
    public com.fasterxml.jackson.databind.Module finishReasonModule() {
        com.fasterxml.jackson.databind.module.SimpleModule module = new com.fasterxml.jackson.databind.module.SimpleModule();

        // We use the class directly now that Maven is fixed
        module.addDeserializer(FinishReason.class, new com.fasterxml.jackson.databind.JsonDeserializer<FinishReason>() {
            @Override
            public FinishReason deserialize(com.fasterxml.jackson.core.JsonParser p,
                                            com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {

                com.fasterxml.jackson.databind.JsonNode node = p.getCodec().readTree(p);
                // Gemini returns either a string "STOP" or an object {"name": "STOP"}
                String value = node.isTextual() ? node.asText() : node.get("name").asText();

                try {
                    // This finds the static field (STOP, SAFETY, etc.) in the FinishReason class
                    return (FinishReason) FinishReason.class.getField(value.toUpperCase()).get(null);
                } catch (Exception e) {
                    return null;
                }
            }
        });
        return module;
    }

    @Bean
    @Primary // This is the magic word that fixes the WebSocket error
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();

        // The "Brute Force" fix for the Google SDK NO_CONSTRUCTOR error
        module.addDeserializer(FinishReason.class, new com.fasterxml.jackson.databind.JsonDeserializer<FinishReason>() {
            @Override
            public FinishReason deserialize(com.fasterxml.jackson.core.JsonParser p,
                                            com.fasterxml.jackson.databind.DeserializationContext ctxt) throws java.io.IOException {

                com.fasterxml.jackson.databind.JsonNode node = p.getCodec().readTree(p);
                // Gemini 3 metadata handling
                String value = node.isTextual() ? node.asText() : node.get("name").asText();

                try {
                    // Reflection bypasses the missing constructor
                    return (FinishReason) FinishReason.class.getField(value.toUpperCase()).get(null);
                } catch (Exception e) {
                    return null;
                }
            }
        });

        mapper.registerModule(module);
        return mapper;
    }

}