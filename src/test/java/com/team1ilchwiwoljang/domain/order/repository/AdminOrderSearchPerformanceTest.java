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

    @Test
    @DisplayName("관리자 주문 검색 조건별 응답 시간을 Markdown 표로 출력한다")
    void givenAdminOrderSearchConditions_whenMeasure_thenPrintMarkdownTable() {
        List<Scenario> scenarios = List.of(
                new Scenario(
                        "주문 상태 + 기간 + 금액 범위",
                        "/api/admins/orders?orderStatus=PAID&startDate=2026-01-01&endDate=2026-06-30&minTotalAmount=10000&maxTotalAmount=50000&page=0&size=10",
                        new AdminOrderSearchCondition(
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 6, 30),
                                OrderStatus.PAID,
                                10_000L,
                                50_000L,
                                null,
                                null
                        )
                ),
                new Scenario(
                        "주문 상태 + 기간 + 금액 범위 + keyword",
                        "/api/admins/orders?orderStatus=PAID&startDate=2026-01-01&endDate=2026-06-30&minTotalAmount=10000&maxTotalAmount=50000&keyword=target&page=0&size=10",
                        new AdminOrderSearchCondition(
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 6, 30),
                                OrderStatus.PAID,
                                10_000L,
                                50_000L,
                                "target",
                                null
                        )
                ),
                new Scenario(
                        "주문 상태 + 기간 + 정확한 주문번호 keyword",
                        "/api/admins/orders?orderStatus=PAID&startDate=2026-01-01&endDate=2026-06-30&keyword=target-order-000002&page=0&size=10",
                        new AdminOrderSearchCondition(
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 6, 30),
                                OrderStatus.PAID,
                                null,
                                null,
                                "target-order-000002",
                                null
                        )
                ),
                new Scenario(
                        "기간 + 금액 범위 + keyword",
                        "/api/admins/orders?startDate=2026-01-01&endDate=2026-06-30&minTotalAmount=10000&maxTotalAmount=50000&keyword=target&page=0&size=10",
                        new AdminOrderSearchCondition(
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 6, 30),
                                null,
                                10_000L,
                                50_000L,
                                "target",
                                null
                        )
                ),
                new Scenario(
                        "주문 상태 + 금액 범위 + keyword",
                        "/api/admins/orders?orderStatus=PAID&minTotalAmount=10000&maxTotalAmount=50000&keyword=target&page=0&size=10",
                        new AdminOrderSearchCondition(
                                null,
                                null,
                                OrderStatus.PAID,
                                10_000L,
                                50_000L,
                                "target",
                                null
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
                                null,
                                null
                        )
                )
        );

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
}
