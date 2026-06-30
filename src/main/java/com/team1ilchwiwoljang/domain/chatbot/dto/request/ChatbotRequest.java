package com.team1ilchwiwoljang.domain.chatbot.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotRequest(

        @NotBlank(message = "메시지를 입력해주세요.")
        @Size(max = 2000, message = "메시지는 2000자 이하로 입력해주세요.")
        String message
) {
}