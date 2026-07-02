package com.team1ilchwiwoljang.domain.chatbot.tool;

import com.team1ilchwiwoljang.domain.chatbot.state.ChatbotProductPageState;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ProductToolTest {

    private ProductTool productTool;

    @Mock
    private ChatbotProductPageState productPageState;

    @Mock
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productTool = new ProductTool(productPageState, productService);
    }

    @Test
    @DisplayName("ToolContext에 conversationId가 없으면 예외가 발생한다")
    void given_missingConversationId_whenGetLatestProducts_thenThrowException() {
        ToolContext toolContext = new ToolContext(Map.of());

        assertThatThrownBy(() -> productTool.getLatestProducts(toolContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("ToolContext에 conversationId가 없습니다.");

        verifyNoInteractions(productPageState, productService);
    }
}