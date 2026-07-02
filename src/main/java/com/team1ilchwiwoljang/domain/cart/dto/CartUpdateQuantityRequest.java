package com.team1ilchwiwoljang.domain.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartUpdateQuantityRequest(
        @NotNull
        @Positive
        Integer quantity
) {
}
