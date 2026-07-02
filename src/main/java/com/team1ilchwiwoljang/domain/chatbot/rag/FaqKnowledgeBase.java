package com.team1ilchwiwoljang.domain.chatbot.rag;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FaqKnowledgeBase {

    /**
     * 처음 RAG를 검증하기 위한 고정 FAQ 목록입니다.
     */
    public List<FaqDocument> findAll() {
        return List.of(
                new FaqDocument(
                        "faq-delivery-001",
                        "배송 기간 안내",
                        "일반 상품은 결제 완료 후 영업일 기준 2~3일 이내에 출고됩니다. 배송은 보통 출고 후 지역에 따라 1~3일 정도 걸릴 수 있습니다."
                ),
                new FaqDocument(
                        "faq-refund-001",
                        "환불 및 반품 가능 기간",
                        "상품 수령 후 7일 이내에 환불 또는 반품을 신청할 수 있습니다. 마음에 들지 않는 상품을 다시 보내고 싶은 경우에도 7일 이내에 신청해야 합니다. 단, 사용 흔적이 있거나 포장이 훼손된 경우 환불이나 반품이 제한될 수 있습니다."
                ),
                new FaqDocument(
                        "faq-cs-001",
                        "고객센터 운영 시간",
                        "고객센터 운영 시간은 평일 09:00~18:00이며, 점심시간은 12:00~13:00입니다. 주말 및 공휴일은 휴무입니다."
                )
        );
    }
}
