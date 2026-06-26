package com.team1ilchwiwoljang.domain.order.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.security.annotation.Auth;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.order.dto.request.CartOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderPreviewRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.DirectOrderPreviewResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderPreviewResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
     * 상품 상세 페이지에서 바로 주문하기 전 주문서 미리보기를 조회합니다.
     */
    @PostMapping("/direct/preview")
    public ResponseEntity<ApiResponse<DirectOrderPreviewResponse>> previewDirectOrder(
            // 현재 프로젝트 정책상 상품 목록/카테고리 조회 외 API는 인증이 필요합니다.
            // 다만 바로 주문 미리보기 계산에는 회원 정보가 필요 없으므로 서비스에는 memberId를 전달하지 않습니다.
            @Auth AuthMember authMember,
            @Valid @RequestBody DirectOrderPreviewRequest request
    ) {
        DirectOrderPreviewResponse response = orderService.previewDirectOrder(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 상품 상세 페이지에서 단건 바로 주문을 생성합니다.
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

    @PostMapping("/carts")
    public ResponseEntity<ApiResponse<OrderResponse>> createCartOrder(
            @Auth AuthMember authMember,
            @Valid @RequestBody CartOrderRequest request
    ) {
        OrderResponse response = orderService.createCartOrder(authMember.memberId(), request);
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
                orderId
        );

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
