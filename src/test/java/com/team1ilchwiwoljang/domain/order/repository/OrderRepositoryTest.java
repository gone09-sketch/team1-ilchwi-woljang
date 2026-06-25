package com.team1ilchwiwoljang.domain.order.repository;

import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import com.team1ilchwiwoljang.domain.order.dto.request.OrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("given_orders_when_findOrderHistoryByMemberId_then_returnPagedOrdersDesc")
    void givenOrders_whenFindOrderHistoryByMemberId_thenReturnPagedOrdersDesc() {
        // given
        Member member = Member.create("user@example.com", "password", "홍길동", "010-1234-5678");
        memberRepository.save(member);

        Order order1 = Order.create(member, "ORD-001", 10000L, 10000L);
        Order order2 = Order.create(member, "ORD-002", 20000L, 20000L);
        Order order3 = Order.create(member, "ORD-003", 30000L, 30000L);

        orderRepository.saveAll(List.of(order1, order2, order3));

        Pageable pageable = PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id"));
        OrderSearchCondition condition = new OrderSearchCondition(null, null, null, null);

        // when
        Page<Order> result = orderRepository.findOrderHistoryByMemberId(member.getId(), condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
        
        // 최신 주문순(id DESC) 정렬 확인
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ORD-003");
        assertThat(result.getContent().get(1).getOrderNumber()).isEqualTo("ORD-002");
    }

    @Test
    @DisplayName("given_noOrders_when_findOrderHistoryByMemberId_then_returnEmptyPage")
    void givenNoOrders_whenFindOrderHistoryByMemberId_thenReturnEmptyPage() {
        // given
        Long nonExistentMemberId = 999L;
        Pageable pageable = PageRequest.of(0, 10);
        OrderSearchCondition condition = new OrderSearchCondition(null, null, null, null);

        // when
        Page<Order> result = orderRepository.findOrderHistoryByMemberId(nonExistentMemberId, condition, pageable);

        // then
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("주문 상태 및 주문 번호, 날짜 조건으로 필터링하여 검색한다")
    void searchOrdersWithFilters() {
        // given
        Member member = Member.create("filter@example.com", "password", "홍길동", "010-1234-5678");
        memberRepository.save(member);

        Order order1 = Order.create(member, "ORD-MATCH-01", 10000L, 10000L);
        Order order2 = Order.create(member, "ORD-MATCH-02", 20000L, 20000L);
        Order order3 = Order.create(member, "ORD-OTHER-03", 30000L, 30000L);

        orderRepository.saveAll(List.of(order1, order2, order3));

        // order2는 취소 상태로 변경
        order2.cancel(java.time.LocalDateTime.now());
        orderRepository.save(order2);

        Pageable pageable = PageRequest.of(0, 10);

        // 1. 주문 번호 검색 필터 ("MATCH")
        OrderSearchCondition cond1 = new OrderSearchCondition(null, null, null, "MATCH");
        Page<Order> result1 = orderRepository.findOrderHistoryByMemberId(member.getId(), cond1, pageable);
        assertThat(result1.getContent()).hasSize(2);
        assertThat(result1.getContent()).extracting("orderNumber").containsExactlyInAnyOrder("ORD-MATCH-01", "ORD-MATCH-02");

        // 2. 주문 상태 검색 필터 ("CANCELLED")
        OrderSearchCondition cond2 = new OrderSearchCondition(null, null, OrderStatus.CANCELLED, null);
        Page<Order> result2 = orderRepository.findOrderHistoryByMemberId(member.getId(), cond2, pageable);
        assertThat(result2.getContent()).hasSize(1);
        assertThat(result2.getContent().get(0).getOrderNumber()).isEqualTo("ORD-MATCH-02");

        // 3. 날짜 범위 검색 필터
        OrderSearchCondition cond3 = new OrderSearchCondition(LocalDate.now(), LocalDate.now(), null, null);
        Page<Order> result3 = orderRepository.findOrderHistoryByMemberId(member.getId(), cond3, pageable);
        assertThat(result3.getContent()).hasSize(3);
    }
}
