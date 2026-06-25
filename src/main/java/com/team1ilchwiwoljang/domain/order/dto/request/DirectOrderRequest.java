package com.team1ilchwiwoljang.domain.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record DirectOrderRequest(
        @NotNull
        Long productId,

        @Min(1)
        int quantity
) {
}
