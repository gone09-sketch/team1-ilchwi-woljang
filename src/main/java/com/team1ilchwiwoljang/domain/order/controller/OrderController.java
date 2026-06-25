package com.team1ilchwiwoljang.domain.order.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.security.annotation.Auth;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderPreviewRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderPreviewResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 상품 상세 페이지에서 바로 주문하기 전 주문서 미리보기를 조회합니다.
     */
    @PostMapping("/direct/preview")
    public ResponseEntity<ApiResponse<OrderPreviewResponse>> previewDirectOrder(
            // 바로 주문 미리보기는 회원별 데이터를 조회하지 않지만,
            // 인증이 필요한 API임을 명시하기 위해 AuthMember를 받습니다.
            @Auth AuthMember authMember,
            @Valid @RequestBody DirectOrderPreviewRequest request
    ) {
        OrderPreviewResponse response = orderService.previewDirectOrder(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

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
}
