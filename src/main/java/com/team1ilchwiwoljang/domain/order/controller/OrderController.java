package com.team1ilchwiwoljang.domain.order.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.security.annotation.Auth;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderPreviewResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 장바구니 상품을 기준으로 주문서 미리보기를 조회합니다.
     * cartIds가 없으면 회원의 전체 장바구니를 대상으로 미리보기를 생성하고,
     * cartIds가 있으면 선택된 장바구니 상품만 대상으로 미리보기를 생성합니다.
     */
    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<OrderPreviewResponse>> previewOrder(
            @Auth AuthMember authMember,
            @RequestParam(required = false) List<Long> cartIds
    ) {

        OrderPreviewResponse response = orderService.previewOrder(
                authMember.memberId(),
                cartIds
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 상품 상세페이지 -> 바로 구매
     * 단 건 주문만 가능
     */
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

}
