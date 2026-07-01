package com.team1ilchwiwoljang.domain.chatbot.dto.response;

import java.util.List;

public record ChatbotProductToolResponse(
        String message,
        List<ChatbotGetProductResponse> products
) {
}
