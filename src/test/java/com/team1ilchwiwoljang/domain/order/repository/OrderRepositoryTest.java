package com.team1ilchwiwoljang.domain.order.repository;

import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import com.team1ilchwiwoljang.domain.order.dto.request.AdminOrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.dto.request.OrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Test
    @DisplayName("given_orders_when_findOrderHistoryByMemberId_then_returnPagedOrdersByCreatedAtDescAndIdDesc")
    void givenOrders_whenFindOrderHistoryByMemberId_thenReturnPagedOrdersByCreatedAtDescAndIdDesc() {
        // given
        Member member = Member.create("user@example.com", "password", "홍길동", "010-1234-5678");
        memberRepository.save(member);

        Order order1 = Order.create(member, "ORD-001", 10000L, 10000L);
        Order order2 = Order.create(member, "ORD-002", 20000L, 20000L);
        Order order3 = Order.create(member, "ORD-003", 30000L, 30000L);

        orderRepository.saveAll(List.of(order1, order2, order3));

        LocalDateTime now = LocalDateTime.now();
        ReflectionTestUtils.setField(order1, "createdAt", now);
        ReflectionTestUtils.setField(order2, "createdAt", now.minusDays(2));
        ReflectionTestUtils.setField(order3, "createdAt", now.minusDays(1));

        Pageable pageable = PageRequest.of(0, 2);
        OrderSearchCondition condition = new OrderSearchCondition(null, null, null, null);

        // when
        Page<Order> result = orderRepository.findOrderHistoryByMemberId(member.getId(), condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);

        // 최신 주문순(createdAt DESC, id DESC) 정렬 확인
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ORD-001");
        assertThat(result.getContent().get(1).getOrderNumber()).isEqualTo("ORD-003");
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
        order2.cancel(LocalDateTime.now());
        orderRepository.save(order2);

        Pageable pageable = PageRequest.of(0, 10);

        // 1. 일반 회원 주문 내역 검색은 기존 OrderSearchCondition.keyword를 사용한다.
        // 이 부분은 관리자 검색 최적화와 별개다.
        OrderSearchCondition cond1 = new OrderSearchCondition(null, null, null, "MATCH");
        Page<Order> result1 = orderRepository.findOrderHistoryByMemberId(member.getId(), cond1, pageable);
        assertThat(result1.getContent()).hasSize(2);
        assertThat(result1.getContent())
                .extracting("orderNumber")
                .containsExactlyInAnyOrder("ORD-MATCH-01", "ORD-MATCH-02");

        // 2. 주문 상태 검색 필터
        OrderSearchCondition cond2 = new OrderSearchCondition(null, null, OrderStatus.CANCELLED, null);
        Page<Order> result2 = orderRepository.findOrderHistoryByMemberId(member.getId(), cond2, pageable);
        assertThat(result2.getContent()).hasSize(1);
        assertThat(result2.getContent().get(0).getOrderNumber()).isEqualTo("ORD-MATCH-02");

        // 3. 날짜 범위 검색 필터
        OrderSearchCondition cond3 = new OrderSearchCondition(LocalDate.now(), LocalDate.now(), null, null);
        Page<Order> result3 = orderRepository.findOrderHistoryByMemberId(member.getId(), cond3, pageable);
        assertThat(result3.getContent()).hasSize(3);
    }

    @Test
    @DisplayName("keyword가 주문 번호에 포함되면 주문 내역을 조회한다")
    void givenKeyword_whenOrderNumberContainsKeyword_thenReturnOrders() {
        // given
        Member member = Member.create("number@example.com", "password", "홍길동", "010-1234-5678");
        memberRepository.save(member);

        Order order1 = Order.create(member, "ORD-123-001", 10000L, 10000L);
        Order order2 = Order.create(member, "ORD-999-001", 20000L, 20000L);
        orderRepository.saveAll(List.of(order1, order2));

        // 일반 회원 주문 내역 검색은 아직 keyword 부분 검색 정책을 유지한다.
        OrderSearchCondition condition = new OrderSearchCondition(null, null, null, "ORD-123");
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Order> result = orderRepository.findOrderHistoryByMemberId(member.getId(), condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ORD-123-001");
    }

    @Test
    @DisplayName("keyword가 주문 상품명에만 포함되면 주문 목록에서 조회하지 않는다")
    void givenKeyword_whenOnlyProductNameContainsKeyword_thenReturnEmptyPage() {
        // given
        Member member = Member.create("product@example.com", "password", "홍길동", "010-1234-5678");
        memberRepository.save(member);

        Product product1 = productRepository.save(Product.create("노트북", 10000, 10, ProductStatus.ON_SALE, "설명", null));
        Product product2 = productRepository.save(Product.create("키보드", 20000, 10, ProductStatus.ON_SALE, "설명", null));
        Product product3 = productRepository.save(Product.create("노트북 거치대", 15000, 10, ProductStatus.ON_SALE, "설명", null));

        Order order1 = orderRepository.save(Order.create(member, "ORD-001", 10000L, 10000L));
        Order order2 = orderRepository.save(Order.create(member, "ORD-002", 20000L, 20000L));

        orderItemRepository.save(OrderItem.create(order1, product1, "게이밍 노트북", 10000L, 1L, 10000L, null, null));
        orderItemRepository.save(OrderItem.create(order1, product3, "노트북 거치대", 15000L, 1L, 15000L, null, null));
        orderItemRepository.save(OrderItem.create(order2, product2, "기계식 키보드", 20000L, 1L, 20000L, null, null));

        // 일반 회원 주문 목록에서는 상품명 검색을 하지 않는 정책 확인
        OrderSearchCondition condition = new OrderSearchCondition(null, null, null, "노트북");
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Order> result = orderRepository.findOrderHistoryByMemberId(member.getId(), condition, pageable);

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("날짜, 주문 상태, keyword 조건을 조합하여 주문 내역을 조회한다")
    void givenDateStatusAndKeyword_whenFindOrderHistory_thenReturnFilteredOrders() {
        // given
        Member member = Member.create("combined@example.com", "password", "홍길동", "010-1234-5678");
        memberRepository.save(member);

        Order pendingOrder = orderRepository.save(Order.create(member, "ORD-123-PENDING", 10000L, 10000L));
        Order cancelledOrder = orderRepository.save(Order.create(member, "ORD-123-CANCELLED", 10000L, 10000L));
        cancelledOrder.cancel(LocalDateTime.now());

        // 일반 회원 주문 내역 검색은 기존 keyword 방식 유지
        OrderSearchCondition condition = new OrderSearchCondition(
                LocalDate.now(),
                LocalDate.now(),
                OrderStatus.CANCELLED,
                "CANCELLED"
        );
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Order> result = orderRepository.findOrderHistoryByMemberId(member.getId(), condition, pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ORD-123-CANCELLED");
    }

    @Test
    @DisplayName("관리자 주문 검색 조건으로 전체 주문을 필터링한다")
    void givenAdminOrderSearchCondition_whenFindAdminOrders_thenReturnFilteredOrders() {
        // given
        Member member1 = Member.create("admin-search-1@example.com", "password", "회원1", "010-1111-1111");
        Member member2 = Member.create("admin-search-2@example.com", "password", "회원2", "010-2222-2222");
        memberRepository.saveAll(List.of(member1, member2));

        LocalDateTime baseTime = LocalDateTime.of(2026, 6, 30, 12, 0);

        Order match1 = orderRepository.save(Order.create(member1, "ADM-MATCH-01", 20000L, 20000L));
        Order match2 = orderRepository.save(Order.create(member2, "ADM-MATCH-02", 30000L, 30000L));
        Order pendingOrder = orderRepository.save(Order.create(member1, "ADM-MATCH-03", 20000L, 20000L));
        Order highAmountOrder = orderRepository.save(Order.create(member1, "ADM-MATCH-04", 800000L, 800000L));
        Order noKeywordOrder = orderRepository.save(Order.create(member1, "ADM-OTHER-05", 20000L, 20000L));
        Order oldOrder = orderRepository.save(Order.create(member1, "ADM-MATCH-06", 20000L, 20000L));

        ReflectionTestUtils.setField(match1, "orderStatus", OrderStatus.PAID);
        ReflectionTestUtils.setField(match1, "createdAt", baseTime);

        ReflectionTestUtils.setField(match2, "orderStatus", OrderStatus.PAID);
        ReflectionTestUtils.setField(match2, "createdAt", baseTime.minusHours(1));

        ReflectionTestUtils.setField(pendingOrder, "createdAt", baseTime);

        ReflectionTestUtils.setField(highAmountOrder, "orderStatus", OrderStatus.PAID);
        ReflectionTestUtils.setField(highAmountOrder, "createdAt", baseTime);

        ReflectionTestUtils.setField(noKeywordOrder, "orderStatus", OrderStatus.PAID);
        ReflectionTestUtils.setField(noKeywordOrder, "createdAt", baseTime);

        ReflectionTestUtils.setField(oldOrder, "orderStatus", OrderStatus.PAID);
        ReflectionTestUtils.setField(oldOrder, "createdAt", baseTime.minusYears(1));

        AdminOrderSearchCondition condition = new AdminOrderSearchCondition(
                baseTime.toLocalDate(),
                baseTime.toLocalDate(),
                OrderStatus.PAID,
                10000L,
                500000L,
                "ADM-MATCH-02", // orderNumber: 주문번호 정확 검색
                null,           // productName: 상품명 검색 안 함
                null            // memberId: 회원 ID 검색 안 함
        );
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Order> result = orderRepository.findAdminOrders(condition, pageable);

        // then
        // orderNumber는 contains가 아니라 eq 조건으로 검색한다.
        // 따라서 ADM-MATCH-01, ADM-MATCH-02를 모두 찾는 것이 아니라
        // 정확히 ADM-MATCH-02 한 건만 조회되어야 한다.
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ADM-MATCH-02");
    }

    @Test
    @DisplayName("관리자 주문 검색은 productName이 주문 상품명에 포함되면 주문을 조회한다")
    void givenAdminProductName_whenProductNameContainsKeyword_thenReturnOrders() {
        // given
        Member member = Member.create("admin-product@example.com", "password", "회원", "010-3333-4444");
        memberRepository.save(member);

        Product product1 = productRepository.save(Product.create("노트북", 10000, 10, ProductStatus.ON_SALE, "설명", null));
        Product product2 = productRepository.save(Product.create("키보드", 20000, 10, ProductStatus.ON_SALE, "설명", null));

        Order order1 = orderRepository.save(Order.create(member, "ADM-PRODUCT-01", 10000L, 10000L));
        Order order2 = orderRepository.save(Order.create(member, "ADM-PRODUCT-02", 20000L, 20000L));

        orderItemRepository.save(OrderItem.create(order1, product1, "게이밍 노트북", 10000L, 1L, 10000L, null, null));
        orderItemRepository.save(OrderItem.create(order2, product2, "기계식 키보드", 20000L, 1L, 20000L, null, null));

        AdminOrderSearchCondition condition = new AdminOrderSearchCondition(
                null,
                null,
                null,
                null,
                null,
                null,           // orderNumber: 주문번호 검색 안 함
                "게이밍 노트북",  // productName: 주문 상품명 검색
                null            // memberId: 회원 ID 검색 안 함
        );
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Order> result = orderRepository.findAdminOrders(condition, pageable);

        // then
        // 관리자 상품명 검색은 order_items.product_name_snapshot을 대상으로 조회한다.
        // 따라서 상품명 스냅샷에 "게이밍 노트북"이 들어간 order1만 조회되어야 한다.
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ADM-PRODUCT-01");
    }

    @Test
    @DisplayName("관리자 주문 검색은 선택 필터 없이 날짜 조건만으로도 조회한다")
    void givenOnlyDateCondition_whenFindAdminOrders_thenReturnOrders() {
        // given
        Member member = Member.create("admin-date@example.com", "password", "회원", "010-3333-3333");
        memberRepository.save(member);

        LocalDateTime baseTime = LocalDateTime.of(2026, 6, 30, 12, 0);
        Order order = orderRepository.save(Order.create(member, "ADM-DATE-01", 20000L, 20000L));
        ReflectionTestUtils.setField(order, "createdAt", baseTime);

        AdminOrderSearchCondition condition = new AdminOrderSearchCondition(
                baseTime.toLocalDate(),
                baseTime.toLocalDate(),
                null,
                null,
                null,
                null, // orderNumber: 주문번호 검색 안 함
                null, // productName: 상품명 검색 안 함
                null  // memberId: 회원 ID 검색 안 함
        );
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Order> result = orderRepository.findAdminOrders(condition, pageable);

        // then
        // 선택 필터가 없어도 날짜 조건만으로 관리자 주문 목록 조회가 가능해야 한다.
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ADM-DATE-01");
    }
}
