package com.team1ilchwiwoljang.domain.chatbot.dto.response;

import com.team1ilchwiwoljang.domain.product.dto.ProductResponse;

public record ChatbotGetProductResponse(
        String name,
        int price
) {
    public static ChatbotGetProductResponse from(ProductResponse product) {
        return new ChatbotGetProductResponse(
                product.name(),
                product.price()
        );
    }
}