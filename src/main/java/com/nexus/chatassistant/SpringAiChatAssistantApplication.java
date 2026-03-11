package com.nexus.chatassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringAiChatAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiChatAssistantApplication.class, args);
    }

}
