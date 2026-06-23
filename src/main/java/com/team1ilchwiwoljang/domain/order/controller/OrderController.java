package com.team1ilchwiwoljang.domain.order.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OrderResponse> createDirectOrder(@Valid @RequestBody DirectOrderRequest request) {
        // JWT 완성되면 @AuthenticationPrincipal로 교체
        Long memberId = 1L;
        OrderResponse response = orderService.createDirectOrder(memberId, request);
        return ApiResponse.of(response);
    }
}
