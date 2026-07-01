package com.team1ilchwiwoljang.domain.chatbot.service;

import com.team1ilchwiwoljang.domain.chatbot.dto.request.ChatbotRequest;
import com.team1ilchwiwoljang.domain.chatbot.dto.response.ChatbotResponse;
import com.team1ilchwiwoljang.domain.chatbot.prompt.ChatbotPromptBuilder;
import com.team1ilchwiwoljang.domain.chatbot.rag.FaqContextService;
import com.team1ilchwiwoljang.domain.chatbot.state.ChatbotConversationState;
import com.team1ilchwiwoljang.domain.chatbot.state.ChatbotProductPageState;
import com.team1ilchwiwoljang.domain.chatbot.tool.ProductTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    private static final String CONVERSATION_ID = "conversation-1";
    private static final String EXPIRED_CONVERSATION_ID = "expired-conversation";

    private ChatbotService chatbotService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private ProductTool productTool;

    @Mock
    private FaqContextService faqContextService;

    @Mock
    private ChatbotPromptBuilder chatbotPromptBuilder;

    @Mock
    private ChatbotConversationState chatbotConversationState;

    @Mock
    private ChatbotProductPageState chatbotProductPageState;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(
                chatClient,
                chatMemory,
                productTool,
                faqContextService,
                chatbotPromptBuilder,
                chatbotConversationState,
                chatbotProductPageState
        );
    }

    @Test
    @DisplayName("만료된 대화가 있으면 ChatMemory와 상품 페이지 상태를 함께 삭제한다")
    void given_expiredConversation_whenChat_thenClearChatMemoryAndProductPageState() {
        ChatbotRequest chatbotRequest = new ChatbotRequest(CONVERSATION_ID, "상품 보여줘");
        ChatbotConversationState.ConversationTouchResult touchResult =
                new ChatbotConversationState.ConversationTouchResult(
                        List.of(EXPIRED_CONVERSATION_ID),
                        false
                );

        given(chatbotConversationState.touch(CONVERSATION_ID)).willReturn(touchResult);
        given(faqContextService.retrieveContext(chatbotRequest.message())).willReturn("");
        given(chatbotPromptBuilder.build("")).willReturn("system prompt");
        givenChatClientResponse("챗봇 응답");

        ChatbotResponse response = chatbotService.chat(chatbotRequest);

        assertThat(response.answer()).isEqualTo("챗봇 응답");
        verify(chatMemory).clear(EXPIRED_CONVERSATION_ID);
        verify(chatbotProductPageState).remove(EXPIRED_CONVERSATION_ID);
    }

    @SuppressWarnings("unchecked")
    private void givenChatClientResponse(String answer) {
        given(chatClient.prompt()
                .system(any(String.class))
                .user(any(String.class))
                .advisors(any(Consumer.class))
                .tools(productTool)
                .toolContext(anyMap())
                .call()
                .content()
        ).willReturn(answer);
    }
}
