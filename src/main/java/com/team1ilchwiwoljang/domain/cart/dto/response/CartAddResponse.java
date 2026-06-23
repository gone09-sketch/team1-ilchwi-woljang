package com.team1ilchwiwoljang.domain.cart.dto.response;

import com.team1ilchwiwoljang.domain.cart.entity.Cart;

public record CartAddResponse(
        Long cartId,
        Long productId,
        String productName,
        int quantity
){

    public static CartAddResponse from(Cart cart){
        return new CartAddResponse(
                cart.getId(),
                cart.getProduct().getId(),
                cart.getProduct().getName(),
                cart.getQuantity()
        );
    }
}
