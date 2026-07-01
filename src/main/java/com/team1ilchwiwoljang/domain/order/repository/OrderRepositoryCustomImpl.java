package com.team1ilchwiwoljang.domain.order.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1ilchwiwoljang.domain.order.dto.request.AdminOrderSearchCondition;
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

        BooleanExpression baseWhere = order.member.id.eq(memberId)
                .and(orderStatusEq(condition.orderStatus()))
                .and(createdAtBetween(condition.startDate(), condition.endDate()));

        // 목록 조회는 Order 테이블만 대상으로 한다. 상품명 검색은 주문 상세 조회/검색에서 분리한다.
        BooleanExpression whereClause = baseWhere.and(orderNumberContains(condition.keyword()));

        List<Order> content = queryFactory
                .select(order)
                .from(order)
                .where(whereClause)
                .orderBy(getOrderSpecifiers(pageable.getSort(), order))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(order.count())
                .from(order)
                .where(whereClause)
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Page<Order> findAdminOrders(AdminOrderSearchCondition condition, Pageable pageable) {
        QOrder order = QOrder.order;

        // 관리자 주문 조회 검색 조건
        BooleanBuilder whereClause = new BooleanBuilder()
                .and(orderStatusEq(condition.orderStatus()))
                .and(createdAtBetween(condition.startDate(), condition.endDate()))
                .and(totalAmountBetween(condition.minTotalAmount(), condition.maxTotalAmount()))
                .and(adminKeywordContains(condition.keyword()))
                .and(memberIdEq(condition.memberId()));

        List<Order> content = queryFactory
                .select(order)
                .from(order)
                .where(whereClause)
                .orderBy(getAdminOrderSpecifiers(pageable.getSort(), order))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(order.count())
                .from(order)
                .where(whereClause)
                .fetchOne();

        long totalCount = (total != null) ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    private BooleanExpression orderStatusEq(OrderStatus orderStatus) {
        return orderStatus != null ? QOrder.order.orderStatus.eq(orderStatus) : null;
    }

    private BooleanExpression orderNumberContains(String keyword) {
        return keyword != null && !keyword.isBlank()
                ? QOrder.order.orderNumber.containsIgnoreCase(keyword)
                : null;
    }

    private BooleanExpression adminKeywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        QOrder order = QOrder.order;
        QOrderItem orderItem = QOrderItem.orderItem;

        return order.orderNumber.containsIgnoreCase(keyword)
                .or(JPAExpressions
                        .selectOne()
                        .from(orderItem)
                        .where(orderItem.order.eq(order)
                                .and(orderItem.productNameSnapshot.containsIgnoreCase(keyword)))
                        .exists());
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

    private BooleanExpression totalAmountBetween(Long minTotalAmount, Long maxTotalAmount) {
        if (minTotalAmount == null && maxTotalAmount == null) {
            return null;
        }
        if (minTotalAmount != null && maxTotalAmount != null) {
            return QOrder.order.totalAmount.between(minTotalAmount, maxTotalAmount);
        }
        if (minTotalAmount != null) {
            return QOrder.order.totalAmount.goe(minTotalAmount);
        }
        return QOrder.order.totalAmount.loe(maxTotalAmount);
    }

    private BooleanExpression memberIdEq(Long memberId) {
        return memberId != null ? QOrder.order.member.id.eq(memberId) : null;
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort, QOrder order) {
        List<OrderSpecifier<?>> specifiers = getSortSpecifiers(sort, order);

        // 기본값: 최신 주문순 (createdAt DESC, id DESC)
        if (specifiers.isEmpty()) {
            specifiers.add(new OrderSpecifier<>(com.querydsl.core.types.Order.DESC, order.createdAt));
            specifiers.add(new OrderSpecifier<>(com.querydsl.core.types.Order.DESC, order.id));
        }

        return specifiers.toArray(new OrderSpecifier<?>[0]);
    }

    private OrderSpecifier<?>[] getAdminOrderSpecifiers(Sort sort, QOrder order) {
        List<OrderSpecifier<?>> specifiers = getSortSpecifiers(sort, order);

        // 관리자 목록은 대용량 조회 인덱싱 실험 기준에 맞춰 기본 정렬을 id DESC로 둔다.
        if (specifiers.isEmpty()) {
            specifiers.add(new OrderSpecifier<>(com.querydsl.core.types.Order.DESC, order.id));
        }

        return specifiers.toArray(new OrderSpecifier<?>[0]);
    }

    private List<OrderSpecifier<?>> getSortSpecifiers(Sort sort, QOrder order) {
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

        return specifiers;
    }
}
