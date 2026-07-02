package com.team1ilchwiwoljang.domain.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotRequest(

        /**
         * 같은 대화인지 구분하는 값
         * 같은 conversationId로 요청하면 Spring AI가 이전 대화 내용을 이어서 참고할 수 있습니다.
         */
        @NotBlank(message = "대화 ID를 입력해주세요.")
        @Size(max = 100, message = "대화 ID는 100자 이하로 입력해주세요.")
        String conversationId,

        /**
         * 사용자가 챗봇에게 실제로 보낸 메시지
         */
        @NotBlank(message = "메시지를 입력해주세요.")
        @Size(max = 2000, message = "메시지는 2000자 이하로 입력해주세요.")
        String message
) {
}