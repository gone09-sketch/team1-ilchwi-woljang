package com.team1ilchwiwoljang.domain.order.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1ilchwiwoljang.domain.order.dto.request.OrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import com.team1ilchwiwoljang.domain.order.entity.QOrder;
import com.team1ilchwiwoljang.domain.order.entity.QOrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Order> findOrderHistoryByMemberId(Long memberId, OrderSearchCondition condition, Pageable pageable) {
        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;

        List<Order> content = queryFactory
                .select(order)
                .distinct()
                .from(order)
                .leftJoin(orderItem).on(orderItem.order.eq(order))
                .where(
                        order.member.id.eq(memberId),
                        orderStatusEq(condition.orderStatus()),
                        keywordLike(condition.keyword(), order, orderItem),
                        createdAtBetween(condition.startDate(), condition.endDate())
                )
                .orderBy(getOrderSpecifiers(pageable.getSort(), order))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(order.id.countDistinct())
                .from(order)
                .leftJoin(orderItem).on(orderItem.order.eq(order))
                .where(
                        order.member.id.eq(memberId),
                        orderStatusEq(condition.orderStatus()),
                        keywordLike(condition.keyword(), order, orderItem),
                        createdAtBetween(condition.startDate(), condition.endDate())
                )
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    private BooleanExpression orderStatusEq(OrderStatus orderStatus) {
        return orderStatus != null ? QOrder.order.orderStatus.eq(orderStatus) : null;
    }

    private BooleanExpression keywordLike(String keyword, QOrder order, QOrderItem orderItem) {
        return keyword != null && !keyword.isBlank()
                ? order.orderNumber.containsIgnoreCase(keyword)
                        .or(orderItem.productNameSnapshot.containsIgnoreCase(keyword))
                : null;
    }

    private BooleanExpression createdAtBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        if (startDate != null && endDate != null) {
            return QOrder.order.createdAt.between(
                    startDate.atStartOfDay(),
                    endDate.atTime(LocalTime.MAX)
            );
        }
        if (startDate != null) {
            return QOrder.order.createdAt.goe(startDate.atStartOfDay());
        }
        return QOrder.order.createdAt.loe(endDate.atTime(LocalTime.MAX));
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
