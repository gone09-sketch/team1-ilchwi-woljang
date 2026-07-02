package com.team1ilchwiwoljang.domain.order.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.order.dto.request.AdminOrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.dto.response.AdminOrderDetailResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.AdminOrderSearchResponse;
import com.team1ilchwiwoljang.domain.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admins/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminOrderSearchResponse>>> getAdminOrders(
            AdminOrderSearchCondition condition,
            Pageable pageable
    ) {
        PageResponse<AdminOrderSearchResponse> response = orderService.getAdminOrders(condition, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> getAdminOrderDetail(
            @PathVariable Long orderId
    ) {
        AdminOrderDetailResponse response = orderService.getAdminOrderDetail(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
