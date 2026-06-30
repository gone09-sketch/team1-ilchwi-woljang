package com.team1ilchwiwoljang.domain.chatbot.tool;

import com.team1ilchwiwoljang.domain.chatbot.dto.response.ChatbotGetProductResponse;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductTool {

    private final ProductService productService;

    @Tool(
            name = "getLatestProducts",
            description = "현재 판매 중인 상품 목록을 최신순으로 조회합니다. 상품명과 가격만 반환합니다."
    )
    public List<ChatbotGetProductResponse> getLatestProducts() {
        return productService.getProducts("newest", 0, 5)
                .getContent()
                .stream()
                .map(ChatbotGetProductResponse::from)
                .toList();
    }
}

