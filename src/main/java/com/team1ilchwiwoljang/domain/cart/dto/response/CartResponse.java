package com.team1ilchwiwoljang.domain.cart.dto.response;

import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        long cartTotalPrice
) {
}
