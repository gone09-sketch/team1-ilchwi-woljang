package com.team1ilchwiwoljang.domain.chatbot.tool;

import com.team1ilchwiwoljang.domain.chatbot.dto.response.ChatbotGetProductResponse;
import com.team1ilchwiwoljang.domain.chatbot.dto.response.ChatbotProductToolResponse;
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

    private static final int PRODUCT_PAGE_SIZE = 5;
    private static final String PRODUCT_STATE_EXPIRED_MESSAGE =
            "이전 상품 목록 정보가 만료되어 처음부터 다시 보여드립니다.";

    private final ChatbotProductPageState productPageState;
    private final ProductService productService;

    @Tool(
            name = "getLatestProducts",
            description = "현재 판매 중인 최신 상품 첫 페이지 5개를 조회합니다. 사용자가 상품 목록을 처음 물어볼 때 사용합니다."
    )
    public ChatbotProductToolResponse getLatestProducts(ToolContext toolContext) {
        // 첫 상품 목록 조회는 항상 0페이지부터 시작하고, 해당 conversationId의 상품 페이지 상태를 초기화합니다.
        String conversationId = getConversationId(toolContext);
        int page = productPageState.firstPage(conversationId);

        return new ChatbotProductToolResponse("", findLatestProducts(page));
    }

    @Tool(
            name = "getMoreLatestProducts",
            description = "이전에 조회한 최신 상품 목록의 다음 페이지 5개를 조회합니다. 사용자가 더 보기, 더 있어요, 다른 상품을 보여달라고 말할 때 사용합니다."
    )
    public ChatbotProductToolResponse getMoreLatestProducts(ToolContext toolContext) {
        // 다음 페이지 조회는 conversationId별로 저장된 상품 페이지 상태를 이어갑니다.
        String conversationId = getConversationId(toolContext);
        ChatbotProductPageState.PageStateResult pageState = productPageState.nextPage(conversationId);

        // 상태가 만료되어 사라졌다면 첫 페이지부터 다시 조회하고, LLM이 사용자에게 안내할 수 있도록 메시지를 같이 반환합니다.
        String message = pageState.expired()
                ? PRODUCT_STATE_EXPIRED_MESSAGE
                : "";

        return new ChatbotProductToolResponse(message, findLatestProducts(pageState.page()));
    }

    private List<ChatbotGetProductResponse> findLatestProducts(int page) {
        return productService.getProducts("newest", page, PRODUCT_PAGE_SIZE)
                .getContent()
                .stream()
                .map(ChatbotGetProductResponse::from)
                .toList();
    }

    /**
     * ChatbotService가 ToolContext에 넣어준 conversationId를 꺼냅니다.
     * conversationId가 빠지면 여러 사용자가 같은 상품 페이지 상태를 공유할 수 있으므로 즉시 예외를 던집니다.
     */
    private String getConversationId(ToolContext toolContext) {
        Object conversationId = toolContext.getContext().get("conversationId");

        if (conversationId == null) {
            throw new IllegalArgumentException("ToolContext에 conversationId가 없습니다.");
        }

        return conversationId.toString();
    }
}
