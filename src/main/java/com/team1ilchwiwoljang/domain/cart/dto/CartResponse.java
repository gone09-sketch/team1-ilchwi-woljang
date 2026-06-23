package com.team1ilchwiwoljang.domain.cart.dto;

import com.team1ilchwiwoljang.domain.cart.entity.Cart;

public record CartResponse(
        Long cartId,
        Long productId,
        String productName,
        int quantity
){

    public static CartResponse from(Cart cart){
        return new CartResponse(
                cart.getId(),
                cart.getProduct().getId(),
                cart.getProduct().getName(),
                cart.getQuantity()
        );
    }
}
