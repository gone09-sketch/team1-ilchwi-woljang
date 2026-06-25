package com.team1ilchwiwoljang.domain.order.repository;

import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
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

        // when
        Page<Order> result = orderRepository.findOrderHistoryByMemberId(member.getId(), pageable);

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

        // when
        Page<Order> result = orderRepository.findOrderHistoryByMemberId(nonExistentMemberId, pageable);

        // then
        assertThat(result.isEmpty()).isTrue();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}
