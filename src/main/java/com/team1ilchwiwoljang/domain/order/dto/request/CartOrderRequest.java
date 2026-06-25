package com.team1ilchwiwoljang.domain.order.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CartOrderRequest(
        // 사용자가 주문 대상으로 선택한 장바구니 상품 ID 목록입니다.
        // 빈 목록은 의도하지 않은 주문으로 이어질 수 있어 요청 검증 단계에서 차단합니다.
        @NotEmpty
        List<@NotNull Long> cartIds
) {
}
