package com.team1ilchwiwoljang.domain.chatbot.state;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatbotProductPageState {

    /**
     * conversationId별로 마지막으로 보여준 상품 페이지를 저장합니다.
     * 상품 페이지 상태의 생명주기는 ChatbotConversationState의 대화 TTL과 맞춥니다.
     * 따라서 여기서는 별도 만료 시간을 두지 않고, 대화 세션이 만료될 때 remove()로 함께 삭제합니다.
     */
    private final Map<String, Integer> latestProductPageByConversation = new ConcurrentHashMap<>();

    /**
     * 상품 목록을 처음 조회할 때 호출합니다.
     * 현재 conversationId는 0페이지부터 새로 시작합니다.
     */
    public int firstPage(String conversationId) {
        latestProductPageByConversation.put(conversationId, 0);

        return 0;
    }

    /**
     * 상품 목록의 다음 페이지를 조회할 때 호출합니다.
     * 기존 상태가 있으면 다음 페이지로 이동합니다.
     * 기존 상태가 없으면 대화 세션 만료로 삭제되었거나 처음부터 "더 보여줘"가 들어온 상황이므로 0페이지부터 다시 시작합니다.
     */
    public PageStateResult nextPage(String conversationId) {
        Integer currentPage = latestProductPageByConversation.get(conversationId);
        if (currentPage == null) {
            latestProductPageByConversation.put(conversationId, 0);
            return new PageStateResult(0, true);
        }

        int nextPage = currentPage + 1;
        latestProductPageByConversation.put(conversationId, nextPage);

        return new PageStateResult(nextPage, false);
    }

    /**
     * conversationId 전체 세션이 만료될 때 상품 페이지 상태도 함께 삭제합니다.
     * ChatMemory만 지우고 상품 페이지 상태를 남기면 "더 보여줘" 요청이 이전 페이지를 이어갈 수 있으므로,
     * 대화 메모리와 상품 페이지 상태를 같은 생명주기로 맞춥니다.
     */
    public void remove(String conversationId) {
        latestProductPageByConversation.remove(conversationId);
    }

    public record PageStateResult(
            int page,
            boolean expired
    ) {
    }
}
