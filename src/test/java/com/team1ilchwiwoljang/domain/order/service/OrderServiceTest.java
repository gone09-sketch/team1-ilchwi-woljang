package com.team1ilchwiwoljang.domain.order.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import com.team1ilchwiwoljang.domain.order.repository.OrderItemRepository;
import com.team1ilchwiwoljang.domain.order.repository.OrderRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderHistoryResponse;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private OrderService orderService;

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-06-25T10:00:00Z"),
            ZoneOffset.UTC
    );

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MemberRepository memberRepository;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                productRepository,
                memberRepository,
                fixedClock
        );
    }

    @Test
    @DisplayName("회원과 상품이 정상 존재하고 재고가 충분하면 바로 주문하기에 성공한다")
    void given_validRequest_whenCreateDirectOrder_thenSuccess() {
        // given
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(1L, 2);

        Member member = Member.create("test@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);

        Product product = Product.create("상품명", 10000, 10, ProductStatus.ON_SALE, "상품 설명", null);
        ReflectionTestUtils.setField(product, "id", 1L);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(productRepository.findById(request.productId())).willReturn(Optional.of(product));

        // when
        OrderResponse response = orderService.createDirectOrder(memberId, request);

        // then
        assertThat(response.orderNumber()).isNotNull();
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(response.totalAmount()).isEqualTo(20000L);
        assertThat(response.orderItems()).hasSize(1);
        assertThat(response.orderItems().get(0).productName()).isEqualTo("상품명");
        assertThat(response.orderItems().get(0).productPrice()).isEqualTo(10000L);
        assertThat(response.orderItems().get(0).quantity()).isEqualTo(2L);
        assertThat(response.orderItems().get(0).totalPrice()).isEqualTo(20000L);

        assertThat(product.getStock()).isEqualTo(8); // 재고 차감 확인

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 주문하려고 하면 MEMBER_NOT_FOUND 예외를 던진다")
    void given_nonExistentMember_whenCreateDirectOrder_thenThrowMemberNotFound() {
        // given
        Long memberId = 999L;
        DirectOrderRequest request = new DirectOrderRequest(1L, 2);

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 주문하려고 하면 PRODUCT_NOT_FOUND 예외를 던진다")
    void given_nonExistentProduct_whenCreateDirectOrder_thenThrowProductNotFound() {
        // given
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(999L, 2);

        Member member = Member.create("test@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(productRepository.findById(request.productId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 재고가 주문 수량보다 부족하면 OUT_OF_STOCK 예외를 던진다")
    void given_insufficientStock_whenCreateDirectOrder_thenThrowOutOfStock() {
        // given
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(1L, 5);

        Member member = Member.create("test@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);

        Product product = Product.create("상품명", 10000, 3, ProductStatus.ON_SALE, "상품 설명", null);
        ReflectionTestUtils.setField(product, "id", 1L);

        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
        given(productRepository.findById(request.productId())).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("주문 소유자가 취소를 요청하면 주문 상태와 취소 시각을 변경한다")
    void given_orderOwner_whenCancelOrder_thenSuccess() {
        // given
        Long memberId = 1L;
        Long orderId = 1L;
        Member member = Member.create("test@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);
        Order order = Order.create(member, "order-123", 20000L, 20000L);
        ReflectionTestUtils.setField(order, "id", orderId);

        given(orderRepository.findByIdAndMemberId(orderId, memberId)).willReturn(Optional.of(order));

        // when
        orderService.cancelOrder(memberId, orderId);

        // then
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCanceledAt()).isEqualTo(LocalDateTime.now(fixedClock));
    }

    @Test
    @DisplayName("존재하지 않거나 소유자가 다른 주문을 취소하려고 하면 ORDER_NOT_FOUND 예외를 던진다")
    void given_notOwnedOrder_whenCancelOrder_thenThrowOrderNotFound() {
        // given
        Long memberId = 1L;
        Long orderId = 999L;

        given(orderRepository.findByIdAndMemberId(orderId, memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(memberId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 취소된 주문을 다시 취소하려고 하면 INVALID_ORDER_STATUS 예외를 던진다")
    void given_cancelledOrder_whenCancelAgain_thenThrowInvalidOrderStatus() {
        // given
        Long memberId = 1L;
        Long orderId = 1L;
        Member member = Member.create("test@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);
        Order order = Order.create(member, "order-123", 20000L, 20000L);
        ReflectionTestUtils.setField(order, "id", orderId);
        order.cancel(LocalDateTime.now(fixedClock));

        given(orderRepository.findByIdAndMemberId(orderId, memberId)).willReturn(Optional.of(order));

        // when & then
        assertThatThrownBy(() -> orderService.cancelOrder(memberId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCanceledAt()).isNotNull();
        assertThat(order.getPaidAt()).isNull();
    }

    @Test
    @DisplayName("회원의 주문 내역이 존재하면 페이징된 주문 내역 DTO 목록을 반환한다")
    void given_validMemberAndOrders_whenGetOrderHistory_thenReturnPagedOrderHistoryResponse() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        
        Member member = Member.create("test@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);

        Order order = Order.create(member, "ORD-123", 20000L, 20000L);
        ReflectionTestUtils.setField(order, "id", 100L);

        Product product = Product.create("노트북 파우치", 20000, 10, ProductStatus.ON_SALE, "설명", null);
        ReflectionTestUtils.setField(product, "id", 10L);

        OrderItem orderItem = OrderItem.create(order, product, "노트북 파우치", 20000L, 1L, 20000L);

        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

        given(orderRepository.findOrderHistoryByMemberId(memberId, pageable)).willReturn(orderPage);
        given(orderItemRepository.findByOrderIdIn(List.of(100L))).willReturn(List.of(orderItem));

        // when
        PageResponse<OrderHistoryResponse> result = orderService.getOrderHistory(memberId, pageable);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).orderId()).isEqualTo(100L);
        assertThat(result.content().get(0).orderNumber()).isEqualTo("ORD-123");
        assertThat(result.content().get(0).orderItems()).hasSize(1);
        assertThat(result.content().get(0).orderItems().get(0).productName()).isEqualTo("노트북 파우치");
        assertThat(result.content().get(0).orderItems().get(0).quantity()).isEqualTo(1L);
    }

    @Test
    @DisplayName("주문 내역이 없는 회원이면 빈 페이지 응답을 반환하고 추가 쿼리를 수행하지 않는다")
    void given_noOrders_whenGetOrderHistory_thenReturnEmptyPageResponse() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(orderRepository.findOrderHistoryByMemberId(memberId, pageable)).willReturn(emptyPage);

        // when
        PageResponse<OrderHistoryResponse> result = orderService.getOrderHistory(memberId, pageable);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }
}
