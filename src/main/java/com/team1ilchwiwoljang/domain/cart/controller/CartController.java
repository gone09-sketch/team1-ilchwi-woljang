package com.team1ilchwiwoljang.domain.cart.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartAddResponse;
import com.team1ilchwiwoljang.domain.cart.dto.CartCreateRequest;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartResponse;
import com.team1ilchwiwoljang.domain.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartAddResponse>> addCartItem(
            @RequestParam Long memberId,
            @Valid @RequestBody CartCreateRequest request
            ){
        CartAddResponse response = addCartItemWithRetry(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    private CartAddResponse addCartItemWithRetry(Long memberId, CartCreateRequest request) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                return cartService.addCartItem(memberId, request);
            } catch (DataIntegrityViolationException e) {
                if (i == maxRetries - 1) {
                    throw e;
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
        throw new DataIntegrityViolationException("Failed to add cart item due to concurrent conflicts");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCarts(@RequestParam Long memberId) {
        CartResponse response = cartService.getCart(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
