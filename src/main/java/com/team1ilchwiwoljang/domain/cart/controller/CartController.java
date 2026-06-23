package com.team1ilchwiwoljang.domain.cart.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.domain.cart.dto.CartCreateRequest;
import com.team1ilchwiwoljang.domain.cart.dto.CartAddResponse;
import com.team1ilchwiwoljang.domain.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartAddResponse>> addCartItem(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CartCreateRequest request
            ){
        CartAddResponse response = cartService.addCartItem(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));

    }


}
