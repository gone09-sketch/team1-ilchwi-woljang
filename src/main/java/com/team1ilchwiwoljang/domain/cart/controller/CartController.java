package com.team1ilchwiwoljang.domain.cart.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.security.annotation.Auth;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.cart.dto.CartCreateRequest;
import com.team1ilchwiwoljang.domain.cart.dto.CartUpdateQuantityRequest;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartAddResponse;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartItemResponse;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartResponse;
import com.team1ilchwiwoljang.domain.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartAddResponse>> addCartItem(
            @Auth AuthMember authMember,
            @Valid @RequestBody CartCreateRequest request
    ) {
        CartAddResponse response = cartService.addCartItem(authMember.memberId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCarts(@Auth AuthMember authMember) {
        CartResponse response = cartService.getCart(authMember.memberId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateCartItemQuantity(
            @Auth AuthMember authMember,
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartUpdateQuantityRequest request
    ) {
        CartItemResponse response = cartService.updateCartItemQuantity(
                authMember.memberId(),
                cartItemId,
                request.quantity()
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<Void>> deleteCartItem(
            @Auth AuthMember authMember,
            @PathVariable Long cartItemId
    ) {
        cartService.deleteCartItem(authMember.memberId(), cartItemId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
