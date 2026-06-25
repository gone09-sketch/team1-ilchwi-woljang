package com.team1ilchwiwoljang.domain.order.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.security.annotation.Auth;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.team1ilchwiwoljang.domain.order.dto.request.OrderSearchCondition;
import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderHistoryResponse;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/direct")
    public ResponseEntity<ApiResponse<OrderResponse>> createDirectOrder(
            @Auth AuthMember authMember,
            @Valid @RequestBody DirectOrderRequest request
    ) {
        OrderResponse response = orderService.createDirectOrder(authMember.memberId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @Auth AuthMember authMember,
            @PathVariable Long orderId
    ) {
        orderService.cancelOrder(
                authMember.memberId(),
                orderId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderHistoryResponse>>> getOrderHistory(
            @Auth AuthMember authMember,
            OrderSearchCondition condition,
            Pageable pageable
    ) {
        PageResponse<OrderHistoryResponse> response = orderService.getOrderHistory(authMember.memberId(), condition, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

}
