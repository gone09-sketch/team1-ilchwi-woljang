package com.team1ilchwiwoljang.domain.order.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.QOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> findOrderHistoryByMemberId(Long memberId, Pageable pageable) {
        QOrder order = QOrder.order;

        List<Order> content = queryFactory
                .selectFrom(order)
                .where(order.member.id.eq(memberId))
                .orderBy(getOrderSpecifiers(pageable.getSort(), order))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(order.count())
                .from(order)
                .where(order.member.id.eq(memberId))
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort, QOrder order) {
        List<OrderSpecifier<?>> specifiers = new ArrayList<>();
        
        if (sort.isSorted()) {
            for (Sort.Order sortOrder : sort) {
                com.querydsl.core.types.Order direction = sortOrder.isAscending() 
                        ? com.querydsl.core.types.Order.ASC 
                        : com.querydsl.core.types.Order.DESC;
                
                if ("id".equals(sortOrder.getProperty())) {
                    specifiers.add(new OrderSpecifier<>(direction, order.id));
                } else if ("createdAt".equals(sortOrder.getProperty())) {
                    specifiers.add(new OrderSpecifier<>(direction, order.createdAt));
                }
            }
        }
        
        // 기본값: 최신 주문순 (id DESC)
        if (specifiers.isEmpty()) {
            specifiers.add(new OrderSpecifier<>(com.querydsl.core.types.Order.DESC, order.id));
        }

        return specifiers.toArray(new OrderSpecifier<?>[0]);
    }
}
