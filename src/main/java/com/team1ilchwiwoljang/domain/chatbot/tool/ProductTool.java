package com.team1ilchwiwoljang.domain.chatbot.tool;

import com.team1ilchwiwoljang.domain.chatbot.dto.response.ChatbotGetProductResponse;
import com.team1ilchwiwoljang.domain.chatbot.state.ChatbotProductPageState;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductTool {


    // 챗봇에서 상품을 한 번에 너무 많이 보여주지 않기 위해 한 페이지 크기를 5로 고정합니다.
    private static final int PRODUCT_PAGE_SIZE = 5;

    // conversationId별로 마지막으로 보여준 상품 페이지를 관리하는 상태 클래스
    private final ChatbotProductPageState productPageState;

    private final ProductService productService;

    @Tool(
            name = "getLatestProducts",
            description = "현재 판매 중인 상품 중 최신 상품 첫 페이지 5개를 조회합니다. 사용자가 상품 목록을 처음 물어볼 때 사용합니다."
    )
    public List<ChatbotGetProductResponse> getLatestProducts(ToolContext toolContext) {
        // ToolContext에서 현재 대화의 conversationId를 꺼내 사용자별 상품 조회 페이지를 구분합니다
        String conversationId = getConversationId(toolContext);

        // 첫 조회는 page를 0으로 초기화합니다.
        int page = productPageState.firstPage(conversationId);

        return productService.getProducts("newest", page, PRODUCT_PAGE_SIZE)
                .getContent()
                .stream()
                .map(ChatbotGetProductResponse::from)
                .toList();
    }

    @Tool(
            name = "getMoreLatestProducts",
            description = "이전에 조회한 최신 상품 목록의 다음 페이지 5개를 조회합니다. 사용자가 더 보기, 더 없어? 등 다른 상품도 보여달라고 말할 때 사용합니다."
    )
    public List<ChatbotGetProductResponse> getMoreLatestProducts(ToolContext toolContext) {
        // 현재 대화의 conversationId를 가져옵니다.
        String conversationId = getConversationId(toolContext);

        // 이전에 보여준 페이지 다음 페이지를 계산합니다.
        int page = productPageState.nextPage(conversationId);

        // 계산된 다음 페이지의 상품 목록을 조회합니다.
        return productService.getProducts("newest", page, PRODUCT_PAGE_SIZE)
                .getContent()
                .stream()
                .map(ChatbotGetProductResponse::from)
                .toList();
    }

    /**
     * ChatbotController에서 toolContext로 넣어준 conversationId를 꺼내는 메서드입니다.
     * conversationId가 없으면 default를 사용합니다.
     * 다만, 실제 요청에서는 ChatbotRequest 검증으로 conversationId를 필수로 받는 것이 좋습니다.
     */
    private String getConversationId(ToolContext toolContext) {
        Object conversationId = toolContext.getContext().get("conversationId");

        if (conversationId == null) {
            return "default";
        }

        return conversationId.toString();
    }
}

