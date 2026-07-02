package com.team1ilchwiwoljang.domain.chatbot.rag;

/**
 * 챗봇이 참고할 FAQ 문맥을 조회하는 진입점입니다.
 */
public interface FaqContextService {

    String retrieveContext(String question);
}
