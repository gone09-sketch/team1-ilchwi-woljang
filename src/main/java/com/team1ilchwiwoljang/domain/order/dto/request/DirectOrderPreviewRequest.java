package com.team1ilchwiwoljang.domain.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 상품 상세 페이지에서 바로 주문 미리보기를 요청할 때 사용하는 DTO입니다.
 */
public record DirectOrderPreviewRequest(

        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,

        @NotNull(message = "주문 수량은 필수입니다.")
        @Min(value = 1, message = "주문 수량은 1개 이상이어야 합니다.")
        Integer quantity
) {
}
