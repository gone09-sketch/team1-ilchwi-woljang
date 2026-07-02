package com.team1ilchwiwoljang.domain.order.repository;

import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("""
            select oi
            from OrderItem oi
            join fetch oi.product p
            where oi.order.id in :orderIds
            """)
    List<OrderItem> findByOrderIdIn(List<Long> orderIds);
}
