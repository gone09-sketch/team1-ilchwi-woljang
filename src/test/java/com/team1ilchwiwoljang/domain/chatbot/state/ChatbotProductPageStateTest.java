package com.team1ilchwiwoljang.domain.chatbot.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotProductPageStateTest {

    private final ChatbotProductPageState productPageState = new ChatbotProductPageState();

    @Test
    @DisplayName("같은 대화에서 상품 목록을 계속 조회하면 다음 페이지로 이동한다")
    void given_sameConversation_whenNextPage_thenMoveToNextPage() {
        productPageState.firstPage("conversation-1");

        ChatbotProductPageState.PageStateResult firstResult = productPageState.nextPage("conversation-1");
        ChatbotProductPageState.PageStateResult secondResult = productPageState.nextPage("conversation-1");

        assertThat(firstResult.page()).isEqualTo(1);
        assertThat(firstResult.expired()).isFalse();
        assertThat(secondResult.page()).isEqualTo(2);
        assertThat(secondResult.expired()).isFalse();
    }

    @Test
    @DisplayName("대화 세션이 삭제되면 상품 페이지 상태도 삭제되어 첫 페이지부터 다시 시작한다")
    void given_removedConversation_whenNextPage_thenRestartFromFirstPage() {
        productPageState.firstPage("conversation-1");
        productPageState.nextPage("conversation-1");

        productPageState.remove("conversation-1");
        ChatbotProductPageState.PageStateResult result = productPageState.nextPage("conversation-1");

        assertThat(result.page()).isEqualTo(0);
        assertThat(result.expired()).isTrue();
    }
}
