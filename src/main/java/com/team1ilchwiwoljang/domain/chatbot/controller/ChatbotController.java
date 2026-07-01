package com.team1ilchwiwoljang.domain.chatbot.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.domain.chatbot.dto.request.ChatbotRequest;
import com.team1ilchwiwoljang.domain.chatbot.dto.response.ChatbotResponse;
import com.team1ilchwiwoljang.domain.chatbot.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @GetMapping("/welcome")
    public ApiResponse<String> welcome() {
        return ApiResponse.success("""
            안녕하세요. 일취월장 고객센터 AI 챗봇입니다.
            무엇을 도와드릴까요?
            """);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ChatbotResponse>> chat(
            @Valid @RequestBody ChatbotRequest chatbotRequest
    ) {
        // Controller는 HTTP 요청과 응답 형식만 담당하고, 챗봇 처리 흐름은 Service에 위임합니다.
        ChatbotResponse response = chatbotService.chat(chatbotRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
