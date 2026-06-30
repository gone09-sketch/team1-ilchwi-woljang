package com.team1ilchwiwoljang.common.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    /**
     * 대화 내용을 저장하는 메모리 Bean입니다.
     * InMemoryChatMemoryRepository:
     * - 서버 메모리에 대화 내용을 저장합니다.
     * - 서버를 재시작하면 저장된 대화 내용은 사라집니다.
     * maxMessages(20): conversationId별로 최근 메시지 20개까지만 기억합니다.
     */
    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(20)
                .build();
    }

    /**
     * 기본 ChatClient 설정입니다.
     * MessageChatMemoryAdvisor:
     * - ChatClient가 LLM을 호출할 때 이전 대화 내용을 함께 넣어줍니다.
     * - Controller에서 넘긴 conversationId 기준으로 대화 기록을 구분합니다.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
