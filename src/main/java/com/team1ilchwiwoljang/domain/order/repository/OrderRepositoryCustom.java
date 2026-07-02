package com.team1ilchwiwoljang.domain.order.repository;

import com.team1ilchwiwoljang.domain.order.dto.request.AdminOrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.dto.request.OrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {
    Page<Order> findOrderHistoryByMemberId(Long memberId, OrderSearchCondition condition, Pageable pageable);

    Page<Order> findAdminOrders(AdminOrderSearchCondition condition, Pageable pageable);
}
