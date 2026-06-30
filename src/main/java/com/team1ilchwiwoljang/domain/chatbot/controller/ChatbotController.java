package com.team1ilchwiwoljang.domain.chatbot.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.domain.chatbot.dto.request.ChatbotRequest;
import com.team1ilchwiwoljang.domain.chatbot.dto.response.ChatbotResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai/chatbot")
public class ChatbotController {

    private final ChatClient chatClient;

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
        // call() 시점에 LLM으로 요청을 전송하고, content()로 텍스트 본문만 꺼냅니다.
        String answer = chatClient.prompt()
                .system("""
                        너는 쇼핑몰 고객센터 챗봇이다.
                        
                        지켜야하는 규칙:
                        - 반드시 사용자의 마지막 메시지와 같은 언어로만 답한다. 한국어 질문일 때만 한국어로 답한다.
                        -한국어는 존댓말로 답한다.
                        -Markdown 기호는 사용하지 않는다.
                        -문장마다 줄바꿈하지 않는다. 다만 내용의 종류가 바뀌거나 답변이 길어질 때는 가독성을 위해 문단을 나눈다.
                        -주문, 배송, 재고처럼 확인이 필요한 정보는 임의로 답하거나 추측하지 말고 상담사 연결을 안내한다.
                        -확인할 수 없다는 안내와 상담원 문의 안내는 서로 다른 줄에 작성한다.
                        -상담 안내가 필요하면 아래 형식을 따른다. 본문과 고객센터 정보 사이에만 빈 줄을 한 번 넣는다.
                        -고객센터 번호와 상담 시간의 라벨도 사용자 언어에 맞게 번역한다.
                        
                        -상담 안내 형식은 구조만 참고하고, 문구는 사용자의 언어로 작성한다.
                        상담 안내 형식:
                        고객 질문에 대한 답변 문장
                        고객센터 문의 추가 안내 문장
                        
                        고객센터 번호: 1588-0000
                        상담 시간: 평일 09:00~18:00, 점심시간 12:00~13:00, 주말 및 공휴일 휴무
                        """)
                .user(chatbotRequest.message())
                .call()
                .content();

        return ResponseEntity.ok(ApiResponse.success(ChatbotResponse.from(answer)));
    }
}
