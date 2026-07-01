package com.team1ilchwiwoljang.domain.chatbot.service;

import com.team1ilchwiwoljang.domain.chatbot.dto.request.ChatbotRequest;
import com.team1ilchwiwoljang.domain.chatbot.dto.response.ChatbotResponse;
import com.team1ilchwiwoljang.domain.chatbot.prompt.ChatbotPromptBuilder;
import com.team1ilchwiwoljang.domain.chatbot.rag.FaqContextService;
import com.team1ilchwiwoljang.domain.chatbot.state.ChatbotConversationState;
import com.team1ilchwiwoljang.domain.chatbot.state.ChatbotProductPageState;
import com.team1ilchwiwoljang.domain.chatbot.tool.ProductTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatbotService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ProductTool productTool;
    private final FaqContextService faqContextService;
    private final ChatbotPromptBuilder chatbotPromptBuilder;
    private final ChatbotConversationState chatbotConversationState;
    private final ChatbotProductPageState chatbotProductPageState;

    /**
     * 챗봇 응답 생성 흐름을 조율합니다.
     * Controller는 HTTP 요청/응답만 처리하고, FAQ 문맥 조회부터 LLM 호출까지의 실제 작업은 여기서 처리합니다.
     */
    public ChatbotResponse chat(ChatbotRequest chatbotRequest) {
        // 사용자가 계속 대화 중인지 판단하기 위해 요청마다 마지막 사용 시간을 갱신합니다.
        // 마지막 사용 후 30분이 지난 conversationId만 ChatMemory와 상품 페이지 상태에서 함께 정리합니다.
        cleanupExpiredConversationStates(chatbotRequest.conversationId());

        // 사용자 질문과 관련 있는 FAQ 문맥을 먼저 조회해 system prompt에 함께 넣습니다.
        // FAQ 조회가 실패해도 챗봇 답변 자체는 계속 진행되도록 안전하게 처리합니다.
        String faqContext = retrieveFaqContextSafely(chatbotRequest.message());

        // 긴 system prompt 조립 책임은 PromptBuilder로 분리해 Service의 흐름을 읽기 쉽게 유지합니다.
        String systemPrompt = chatbotPromptBuilder.build(faqContext);

        // ChatMemory는 conversationId를 기준으로 이전 대화 내용을 이어서 참고합니다.
        // 같은 conversationId가 들어오면 같은 대화 흐름으로 처리됩니다.
        String answer = chatClient.prompt()
                .system(systemPrompt)
                .user(chatbotRequest.message())
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID,
                        chatbotRequest.conversationId()
                ))
                .tools(productTool)

                // ProductTool도 같은 conversationId를 알아야 "더 보여줘" 같은 후속 요청의 페이지 상태를 이어갈 수 있습니다.
                .toolContext(Map.of(
                        "conversationId", chatbotRequest.conversationId()
                ))
                .call()
                .content();

        // 외부에는 ChatClient의 원문 문자열 대신 챗봇 응답 DTO 형태로 반환합니다.
        return ChatbotResponse.from(answer);
    }

    private void cleanupExpiredConversationStates(String conversationId) {
        ChatbotConversationState.ConversationTouchResult touchResult =
                chatbotConversationState.touch(conversationId);

        touchResult.expiredConversationIds()
                .forEach(this::clearConversationState);

        if (touchResult.currentConversationExpired()) {
            log.info("챗봇 대화 상태가 만료되어 새 대화로 다시 시작합니다. conversationId={}", conversationId);
        }
    }

    private void clearConversationState(String conversationId) {
        // Spring AI가 기억하는 이전 대화 문맥을 삭제합니다.
        chatMemory.clear(conversationId);

        // 상품 목록 페이지 상태도 같이 지워야 "더 보여줘"가 만료 전 페이지를 이어가지 않습니다.
        chatbotProductPageState.remove(conversationId);
    }

    /**
     * FAQ 검색 실패가 챗봇 전체 실패로 이어지지 않도록 빈 문맥으로 대체합니다.
     * FAQ는 답변 품질을 높이기 위한 보조 정보이므로, 조회 실패만으로 사용자 요청을 실패 처리하지 않습니다.
     */
    private String retrieveFaqContextSafely(String message) {
        try {
            return faqContextService.retrieveContext(message);
        } catch (Exception e) {
            log.warn("FAQ RAG 문맥 조회에 실패했습니다. FAQ 문맥 없이 계속 진행합니다.", e);
            return "";
        }
    }
}
