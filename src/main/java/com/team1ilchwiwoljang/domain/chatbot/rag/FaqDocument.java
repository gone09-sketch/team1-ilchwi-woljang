package com.team1ilchwiwoljang.domain.chatbot.rag;

/**
 * 챗봇이 참고할 FAQ 문서 한 조각입니다.
 * 질문/답변 또는 제목/본문 단위로 잘게 나눠 저장합니다.
 */
public record FaqDocument(
        String id,
        String title,
        String content
) {

    /**
     * embedding을 만들 때 사용할 텍스트입니다.
     * 제목과 본문을 함께 넣어야
     * 사용자의 질문이 제목에 가까워도, 본문에 가까워도 검색될 가능성이 높아집니다.
     */
    public String textForEmbedding() {
        return title + "\n" + content;
    }
}
