package com.team1ilchwiwoljang.domain.order.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team1ilchwiwoljang.domain.order.dto.request.AdminOrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.dto.request.OrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import com.team1ilchwiwoljang.domain.order.entity.QOrder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Order> findOrderHistoryByMemberId(Long memberId, OrderSearchCondition condition, Pageable pageable) {
        QOrder order = QOrder.order;

        BooleanExpression baseWhere = order.member.id.eq(memberId)
                .and(orderStatusEq(condition.orderStatus()))
                .and(createdAtBetween(condition.startDate(), condition.endDate()));

        // 일반 사용자 주문 내역 검색은 주문번호 keyword 기준으로 조회한다.
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

        long totalCount = total != null ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    @Override
    public Page<Order> findAdminOrders(AdminOrderSearchCondition condition, Pageable pageable) {
        // 상품명 검색은 MySQL FULLTEXT INDEX를 사용해야 하므로 native query로 분리한다.
        // QueryDSL JPA에서 MATCH ... AGAINST를 booleanTemplate으로 넣으면
        // Hibernate HQL 파서가 AGAINST 문법을 해석하지 못해 SyntaxException이 발생한다.
        if (condition.productName() != null && !condition.productName().isBlank()) {
            return findAdminOrdersByProductNameNative(condition, pageable);
        }

        QOrder order = QOrder.order;

        // 관리자 주문 조회 검색 조건
        // - orderNumber: 정확히 일치하는 주문번호 검색 (=)
        // - productName: native query에서 FULLTEXT 검색으로 별도 처리
        BooleanBuilder whereClause = new BooleanBuilder()
                .and(orderStatusEq(condition.orderStatus()))
                .and(createdAtBetween(condition.startDate(), condition.endDate()))
                .and(totalAmountBetween(condition.minTotalAmount(), condition.maxTotalAmount()))
                .and(orderNumberEq(condition.orderNumber()))
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

        long totalCount = total != null ? total : 0L;

        return new PageImpl<>(content, pageable, totalCount);
    }

    private Page<Order> findAdminOrdersByProductNameNative(AdminOrderSearchCondition condition, Pageable pageable) {
        boolean h2Database = isH2Database();
        String productNameCondition = h2Database
                ? "LOWER(oi.product_name_snapshot) LIKE :productName"
                : "MATCH(oi.product_name_snapshot) AGAINST (:productName IN BOOLEAN MODE)";

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT o.*
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE %s
                """.formatted(productNameCondition));

        StringBuilder countSql = new StringBuilder("""
                SELECT COUNT(DISTINCT o.id)
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                WHERE %s
                """.formatted(productNameCondition));

        appendAdminOrderWhereForNative(sql, condition);
        appendAdminOrderWhereForNative(countSql, condition);

        sql.append(" ORDER BY o.id DESC LIMIT :limit OFFSET :offset");

        Query contentQuery = entityManager.createNativeQuery(sql.toString(), Order.class);
        Query countQuery = entityManager.createNativeQuery(countSql.toString());

        bindAdminOrderParams(contentQuery, condition);
        bindAdminOrderParams(countQuery, condition);

        String productNameParam = h2Database
                ? "%" + condition.productName().trim().toLowerCase(Locale.ROOT) + "%"
                : condition.productName().trim();
        contentQuery.setParameter("productName", productNameParam);
        countQuery.setParameter("productName", productNameParam);

        contentQuery.setParameter("limit", pageable.getPageSize());
        contentQuery.setParameter("offset", pageable.getOffset());

        @SuppressWarnings("unchecked")
        List<Order> content = contentQuery.getResultList();

        Number total = (Number) countQuery.getSingleResult();

        return new PageImpl<>(content, pageable, total.longValue());
    }

    private boolean isH2Database() {
        return entityManager.unwrap(Session.class).doReturningWork(connection ->
                connection.getMetaData()
                        .getDatabaseProductName()
                        .toLowerCase(Locale.ROOT)
                        .contains("h2")
        );
    }

    private void appendAdminOrderWhereForNative(StringBuilder sql, AdminOrderSearchCondition condition) {
        if (condition.orderStatus() != null) {
            sql.append(" AND o.order_status = :orderStatus");
        }

        if (condition.startDate() != null) {
            sql.append(" AND o.created_at >= :startDate");
        }

        if (condition.endDate() != null) {
            sql.append(" AND o.created_at <= :endDate");
        }

        if (condition.minTotalAmount() != null) {
            sql.append(" AND o.total_amount >= :minTotalAmount");
        }

        if (condition.maxTotalAmount() != null) {
            sql.append(" AND o.total_amount <= :maxTotalAmount");
        }

        if (condition.orderNumber() != null && !condition.orderNumber().isBlank()) {
            sql.append(" AND o.order_number = :orderNumber");
        }

        if (condition.memberId() != null) {
            sql.append(" AND o.member_id = :memberId");
        }
    }

    private void bindAdminOrderParams(Query query, AdminOrderSearchCondition condition) {
        if (condition.orderStatus() != null) {
            query.setParameter("orderStatus", condition.orderStatus().name());
        }

        if (condition.startDate() != null) {
            query.setParameter("startDate", condition.startDate().atStartOfDay());
        }

        if (condition.endDate() != null) {
            query.setParameter("endDate", condition.endDate().atTime(LocalTime.MAX));
        }

        if (condition.minTotalAmount() != null) {
            query.setParameter("minTotalAmount", condition.minTotalAmount());
        }

        if (condition.maxTotalAmount() != null) {
            query.setParameter("maxTotalAmount", condition.maxTotalAmount());
        }

        if (condition.orderNumber() != null && !condition.orderNumber().isBlank()) {
            query.setParameter("orderNumber", condition.orderNumber().trim());
        }

        if (condition.memberId() != null) {
            query.setParameter("memberId", condition.memberId());
        }
    }

    private BooleanExpression orderStatusEq(OrderStatus orderStatus) {
        return orderStatus != null ? QOrder.order.orderStatus.eq(orderStatus) : null;
    }

    private BooleanExpression orderNumberContains(String keyword) {
        return keyword != null && !keyword.isBlank()
                ? QOrder.order.orderNumber.containsIgnoreCase(keyword.trim())
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

    // 관리자 주문번호 검색은 부분 검색이 아니라 정확히 일치하는 값으로 검색한다.
    // containsIgnoreCase를 쓰면 LOWER(order_number) LIKE '%keyword%'로 나가서 인덱스를 타기 어렵다.
    private BooleanExpression orderNumberEq(String orderNumber) {
        return orderNumber != null && !orderNumber.isBlank()
                ? QOrder.order.orderNumber.eq(orderNumber.trim())
                : null;
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
