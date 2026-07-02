package com.team1ilchwiwoljang.domain.order.repository;

import com.team1ilchwiwoljang.domain.order.dto.request.AdminOrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SpringBootTest
@Transactional(readOnly = true)
@EnabledIfEnvironmentVariable(named = "PERFORMANCE_TEST", matches = "true")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.format_sql=false"
})
class AdminOrderSearchPerformanceTest {

    private static final int WARM_UP_COUNT = 1;
    private static final int MEASURE_COUNT = 3;
    private static final Pageable PAGEABLE = PageRequest.of(0, 10);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("관리자 주문 검색 조건별 응답 시간을 Markdown 표로 출력한다")
    void givenAdminOrderSearchConditions_whenMeasure_thenPrintMarkdownTable() {
        List<Scenario> scenarios = scenarios();

        System.out.println();
        System.out.println("warm-up: " + WARM_UP_COUNT + ", measure: " + MEASURE_COUNT + ", page: 0, size: 10, sort: default(id DESC)");
        System.out.println("| 시나리오 | 요청 | 평균(ms) | 중앙값(ms) | 최소(ms) | 최대(ms) | totalElements |");
        System.out.println("|---|---|---:|---:|---:|---:|---:|");

        for (Scenario scenario : scenarios) {
            Measurement measurement = measure(scenario.condition());
            System.out.printf(
                    Locale.KOREA,
                    "| %s | `%s` | %.1f | %d | %d | %d | %d |%n",
                    scenario.name(),
                    scenario.requestPath(),
                    measurement.averageMs(),
                    measurement.medianMs(),
                    measurement.minMs(),
                    measurement.maxMs(),
                    measurement.totalElements()
            );
        }

        System.out.println();
        System.out.println("SQL 단위 측정: content query와 count query를 분리해 측정");
        System.out.println("| 시나리오 | 쿼리 | 평균(ms) | 중앙값(ms) | 최소(ms) | 최대(ms) | 결과 수 |");
        System.out.println("|---|---|---:|---:|---:|---:|---:|");

        for (Scenario scenario : scenarios) {
            QueryMeasurements measurements = measureQueries(scenario.condition());
            printQueryMeasurement(scenario.name(), "목록 조회", measurements.content());
            printQueryMeasurement(scenario.name(), "count", measurements.count());
        }
    }

    private List<Scenario> scenarios() {
        return List.of(
                new Scenario(
                        "주문 상태 + 기간 + 금액 범위",
                        "/api/admins/orders?orderStatus=PAID&startDate=2026-01-01&endDate=2026-06-30&minTotalAmount=10000&maxTotalAmount=50000&page=0&size=10",
                        new AdminOrderSearchCondition(
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 6, 30),
                                OrderStatus.PAID,
                                10_000L,
                                50_000L,
                                null, // orderNumber
                                null, // productName
                                null  // memberId
                        )
                ),
                new Scenario(
                        "주문 상태 + 기간 + 금액 범위 + 상품명",
                        "/api/admins/orders?orderStatus=PAID&startDate=2026-01-01&endDate=2026-06-30&minTotalAmount=10000&maxTotalAmount=50000&productName=target&page=0&size=10",
                        new AdminOrderSearchCondition(
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 6, 30),
                                OrderStatus.PAID,
                                10_000L,
                                50_000L,
                                null,     // orderNumber
                                "target", // productName
                                null      // memberId
                        )
                ),
                new Scenario(
                        "주문 상태 + 기간 + 정확한 주문번호",
                        "/api/admins/orders?orderStatus=PAID&startDate=2026-01-01&endDate=2026-06-30&orderNumber=target-order-000002&page=0&size=10",
                        new AdminOrderSearchCondition(
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 6, 30),
                                OrderStatus.PAID,
                                null,
                                null,
                                "target-order-000002", // orderNumber
                                null,                  // productName
                                null                   // memberId
                        )
                ),
                new Scenario(
                        "기간 + 금액 범위 + 상품명",
                        "/api/admins/orders?startDate=2026-01-01&endDate=2026-06-30&minTotalAmount=10000&maxTotalAmount=50000&productName=target&page=0&size=10",
                        new AdminOrderSearchCondition(
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 6, 30),
                                null,
                                10_000L,
                                50_000L,
                                null,     // orderNumber
                                "target", // productName
                                null      // memberId
                        )
                ),
                new Scenario(
                        "주문 상태 + 금액 범위 + 상품명",
                        "/api/admins/orders?orderStatus=PAID&minTotalAmount=10000&maxTotalAmount=50000&productName=target&page=0&size=10",
                        new AdminOrderSearchCondition(
                                null,
                                null,
                                OrderStatus.PAID,
                                10_000L,
                                50_000L,
                                null,     // orderNumber
                                "target", // productName
                                null      // memberId
                        )
                ),
                new Scenario(
                        "취소 주문 상태 + 기간 + 금액 범위",
                        "/api/admins/orders?orderStatus=CANCELLED&startDate=2026-01-01&endDate=2026-06-30&minTotalAmount=10000&maxTotalAmount=50000&page=0&size=10",
                        new AdminOrderSearchCondition(
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 6, 30),
                                OrderStatus.CANCELLED,
                                10_000L,
                                50_000L,
                                null, // orderNumber
                                null, // productName
                                null  // memberId
                        )
                )
        );
    }
    private Measurement measure(AdminOrderSearchCondition condition) {
        for (int i = 0; i < WARM_UP_COUNT; i++) {
            orderRepository.findAdminOrders(condition, PAGEABLE);
            entityManager.clear();
        }

        List<Long> elapsedTimes = new ArrayList<>();
        long totalElements = 0;

        for (int i = 0; i < MEASURE_COUNT; i++) {
            long startedAt = System.nanoTime();
            Page<Order> result = orderRepository.findAdminOrders(condition, PAGEABLE);
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;

            elapsedTimes.add(elapsedMs);
            totalElements = result.getTotalElements();
            entityManager.clear();
        }

        return Measurement.from(elapsedTimes, totalElements);
    }

    private QueryMeasurements measureQueries(AdminOrderSearchCondition condition) {
        Query contentQuery = createContentQuery(condition);
        Query countQuery = createCountQuery(condition);

        return new QueryMeasurements(
                measureQuery(() -> jdbcTemplate.queryForList(contentQuery.sql(), contentQuery.params().toArray()).size()),
                measureQuery(() -> jdbcTemplate.queryForObject(countQuery.sql(), Long.class, countQuery.params().toArray()))
        );
    }

    private Measurement measureQuery(QueryRunner runner) {
        for (int i = 0; i < WARM_UP_COUNT; i++) {
            runner.run();
        }

        List<Long> elapsedTimes = new ArrayList<>();
        long resultCount = 0;

        for (int i = 0; i < MEASURE_COUNT; i++) {
            long startedAt = System.nanoTime();
            resultCount = runner.run();
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            elapsedTimes.add(elapsedMs);
        }

        return Measurement.from(elapsedTimes, resultCount);
    }

    private Query createContentQuery(AdminOrderSearchCondition condition) {
        QueryParts queryParts = createQueryParts(condition);
        List<Object> params = new ArrayList<>(queryParts.params());
        params.add(PAGEABLE.getPageSize());
        params.add(PAGEABLE.getOffset());

        return new Query("""
                %s
                    o.id,
                    o.canceled_at,
                    o.created_at,
                    o.member_id,
                    o.order_number,
                    o.order_status,
                    o.paid_at,
                    o.pg_amount,
                    o.total_amount,
                    o.updated_at
                %s
                %s
                ORDER BY o.id DESC
                LIMIT ? OFFSET ?
                """.formatted(queryParts.selectKeyword(), queryParts.fromClause(), queryParts.whereClause()), params);
    }

    private Query createCountQuery(AdminOrderSearchCondition condition) {
        QueryParts queryParts = createQueryParts(condition);
        return new Query("""
                SELECT %s
                %s
                %s
                """.formatted(queryParts.countExpression(), queryParts.fromClause(), queryParts.whereClause()), queryParts.params());
    }

    private QueryParts createQueryParts(AdminOrderSearchCondition condition) {
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        boolean productNameSearch = condition.productName() != null && !condition.productName().isBlank();

        String selectKeyword = productNameSearch ? "SELECT DISTINCT" : "SELECT";
        String countExpression = productNameSearch ? "COUNT(DISTINCT o.id)" : "COUNT(o.id)";
        String fromClause = productNameSearch
                ? """
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                """
                : "FROM orders o";

        if (productNameSearch) {
            conditions.add("MATCH(oi.product_name_snapshot) AGAINST (? IN BOOLEAN MODE)");
            params.add(condition.productName().trim());
        }

        if (condition.orderStatus() != null) {
            conditions.add("o.order_status = ?");
            params.add(condition.orderStatus().name());
        }

        if (condition.startDate() != null && condition.endDate() != null) {
            conditions.add("o.created_at BETWEEN ? AND ?");
            params.add(condition.startDate().atStartOfDay());
            params.add(condition.endDate().atTime(23, 59, 59, 999_999_999));
        } else if (condition.startDate() != null) {
            conditions.add("o.created_at >= ?");
            params.add(condition.startDate().atStartOfDay());
        } else if (condition.endDate() != null) {
            conditions.add("o.created_at <= ?");
            params.add(condition.endDate().atTime(23, 59, 59, 999_999_999));
        }

        if (condition.minTotalAmount() != null && condition.maxTotalAmount() != null) {
            conditions.add("o.total_amount BETWEEN ? AND ?");
            params.add(condition.minTotalAmount());
            params.add(condition.maxTotalAmount());
        } else if (condition.minTotalAmount() != null) {
            conditions.add("o.total_amount >= ?");
            params.add(condition.minTotalAmount());
        } else if (condition.maxTotalAmount() != null) {
            conditions.add("o.total_amount <= ?");
            params.add(condition.maxTotalAmount());
        }

        if (condition.orderNumber() != null && !condition.orderNumber().isBlank()) {
            conditions.add("o.order_number = ?");
            params.add(condition.orderNumber().trim());
        }

        if (condition.memberId() != null) {
            conditions.add("o.member_id = ?");
            params.add(condition.memberId());
        }

        if (conditions.isEmpty()) {
            return new QueryParts(selectKeyword, countExpression, fromClause, "", params);
        }

        return new QueryParts(selectKeyword, countExpression, fromClause, "WHERE " + String.join("\n  AND ", conditions), params);
    }

    private void printQueryMeasurement(String scenarioName, String queryName, Measurement measurement) {
        System.out.printf(
                Locale.KOREA,
                "| %s | %s | %.1f | %d | %d | %d | %d |%n",
                scenarioName,
                queryName,
                measurement.averageMs(),
                measurement.medianMs(),
                measurement.minMs(),
                measurement.maxMs(),
                measurement.totalElements()
        );
    }

    private record Scenario(
            String name,
            String requestPath,
            AdminOrderSearchCondition condition
    ) {
    }

    private record Measurement(
            double averageMs,
            long medianMs,
            long minMs,
            long maxMs,
            long totalElements
    ) {

        private static Measurement from(List<Long> elapsedTimes, long totalElements) {
            List<Long> sortedTimes = new ArrayList<>(elapsedTimes);
            Collections.sort(sortedTimes);

            double averageMs = elapsedTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0);

            return new Measurement(
                    averageMs,
                    sortedTimes.get(sortedTimes.size() / 2),
                    sortedTimes.get(0),
                    sortedTimes.get(sortedTimes.size() - 1),
                    totalElements
            );
        }
    }

    @FunctionalInterface
    private interface QueryRunner {
        long run();
    }

    private record Query(String sql, List<Object> params) {
    }

    private record QueryParts(
            String selectKeyword,
            String countExpression,
            String fromClause,
            String whereClause,
            List<Object> params
    ) {
    }

    private record QueryMeasurements(Measurement content, Measurement count) {
    }
}
