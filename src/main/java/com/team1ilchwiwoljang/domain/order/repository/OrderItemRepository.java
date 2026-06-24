package com.team1ilchwiwoljang.domain.order.repository;

import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
