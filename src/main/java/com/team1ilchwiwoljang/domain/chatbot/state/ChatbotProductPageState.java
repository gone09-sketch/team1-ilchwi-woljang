package com.team1ilchwiwoljang.domain.chatbot.state;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatbotProductPageState {

    /**
     * conversationId별로 마지막으로 조회한 최신 상품 목록 페이지를 저장합니다.
     * ConcurrentHashMap:
     * - 여러 사용자가 동시에 챗봇을 사용 가능
     * - 일반 HashMap에 비해 동시에 접근해도 비교적 안전
     */
    private final Map<String, Integer> latestProductPageByConversation = new ConcurrentHashMap<>();

    /**
     * 상품 목록을 처음 조회할 때 호출합니다.
     * 같은 첫 조회 요청에서는 항상 0페이지부터 보여주고,
     * 해당 conversationId의 현재 페이지를 0으로 저장합니다.
     */
    public int firstPage(String conversationId) {
        latestProductPageByConversation.put(conversationId, 0);
        return 0;
    }

    /**
     * 상품 목록을 이어서 조회할 때 호출합니다.
     * 기존 저장값이 0이면 -> 1을 저장하고 반환
     * 기존 저장값이 1이면 -> 2를 저장하고 반환
     */
    public int nextPage(String conversationId) {
        return latestProductPageByConversation.merge(
                conversationId,
                1,
                Integer::sum
        );
    }
}

