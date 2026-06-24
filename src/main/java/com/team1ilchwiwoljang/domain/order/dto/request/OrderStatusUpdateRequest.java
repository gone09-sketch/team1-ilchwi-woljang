package com.team1ilchwiwoljang.domain.order.dto.request;

import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderStatusUpdateRequest (
        @NotNull(message = "주문 상태는 필수입니다.")
        OrderStatus orderStatus

){}
