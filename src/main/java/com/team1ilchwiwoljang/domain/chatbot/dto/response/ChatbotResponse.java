package com.team1ilchwiwoljang.domain.chatbot.dto.response;

public record ChatbotResponse(
        String answer
) {

    public static ChatbotResponse from(String answer) {
        return new ChatbotResponse(answer);
    }
}
