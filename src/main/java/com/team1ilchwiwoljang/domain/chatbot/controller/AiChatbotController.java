package com.team1ilchwiwoljang.domain.chatbot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/chatbot")
public class AiChatbotController {

    private final ChatClient chatClient;

    @GetMapping
    public String questionAI(@RequestParam String message) {
        // call() 시점에 LLM으로 요청을 전송
        ChatClient.CallResponseSpec responseSpec = chatClient.prompt()
                .user(message)
                .call();

        // content()는 LLM 응답에서 텍스트 본문만 꺼내 반환
        String content = responseSpec.content();
        return content;
    }
}
