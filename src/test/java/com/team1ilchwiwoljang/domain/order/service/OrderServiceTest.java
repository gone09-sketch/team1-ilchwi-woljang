package com.team1ilchwiwoljang.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.service.CartService;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.order.dto.request.CartOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderPreviewRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.AdminOrderDetailResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.DirectOrderPreviewResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderPreviewResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import com.team1ilchwiwoljang.domain.order.repository.OrderItemRepository;
import com.team1ilchwiwoljang.domain.order.repository.OrderRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.order.dto.request.AdminOrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.dto.request.OrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.dto.response.AdminOrderSearchResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderHistoryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import static org.mockito.ArgumentMatchers.eq;

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
    private MemberService memberService;

    @Mock
    private ProductService productService;

    @Mock
    private CartService cartService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(
                orderRepository,
                orderItemRepository,
                fixedClock,
                memberService,
                productService,
                cartService
        );
    }

    @Test
    @DisplayName("판매 중인 상품과 충분한 재고가 있으면 바로 구매 미리보기에 성공한다")
    void given_validRequest_whenPreviewDirectOrder_thenSuccess() {
        DirectOrderPreviewRequest request = new DirectOrderPreviewRequest(1L, 2);
        Product product = createProduct(1L, "product", 10_000, 10, ProductStatus.ON_SALE);
        given(productService.getProduct(request.productId())).willReturn(product);

        DirectOrderPreviewResponse response = orderService.previewDirectOrder(request);

        assertThat(response.totalOrderAmount()).isEqualTo(20_000L);
        assertThat(response.orderItems()).hasSize(1);
        assertThat(response.orderItems().get(0).productId()).isEqualTo(1L);
        assertThat(response.orderItems().get(0).productName()).isEqualTo("product");
        assertThat(response.orderItems().get(0).productPrice()).isEqualTo(10_000L);
        assertThat(response.orderItems().get(0).quantity()).isEqualTo(2);
        assertThat(response.orderItems().get(0).productTotalAmount()).isEqualTo(20_000L);
        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 바로 구매 미리보기를 요청하면 PRODUCT_NOT_FOUND 예외를 던진다")
    void given_nonExistentProduct_whenPreviewDirectOrder_thenThrowProductNotFound() {
        DirectOrderPreviewRequest request = new DirectOrderPreviewRequest(999L, 2);
        given(productService.getProduct(request.productId()))
                .willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        assertThatThrownBy(() -> orderService.previewDirectOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("판매 중이 아닌 상품으로 바로 구매 미리보기를 요청하면 NOT_ORDERABLE_PRODUCT 예외를 던진다")
    void given_notOrderableProduct_whenPreviewDirectOrder_thenThrowNotOrderableProduct() {
        DirectOrderPreviewRequest request = new DirectOrderPreviewRequest(1L, 2);
        Product product = createProduct(1L, "product", 10_000, 10, ProductStatus.STOPPED);
        given(productService.getProduct(request.productId())).willReturn(product);

        assertThatThrownBy(() -> orderService.previewDirectOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ORDERABLE_PRODUCT);
    }

    @Test
    @DisplayName("상품 재고가 미리보기 수량보다 부족하면 OUT_OF_STOCK 예외를 던진다")
    void given_insufficientStock_whenPreviewDirectOrder_thenThrowOutOfStock() {
        DirectOrderPreviewRequest request = new DirectOrderPreviewRequest(1L, 5);
        Product product = createProduct(1L, "product", 10_000, 3, ProductStatus.ON_SALE);
        given(productService.getProduct(request.productId())).willReturn(product);

        assertThatThrownBy(() -> orderService.previewDirectOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("정상적인 바로 주문 요청이면 주문을 생성하고 상품 재고를 차감한다")
    void given_validRequest_whenCreateDirectOrder_thenSuccess() {
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(1L, 2);
        Member member = createMember(memberId);
        Product product = createProduct(1L, "product", 10_000, 10, ProductStatus.ON_SALE);
        given(memberService.getMember(memberId)).willReturn(member);
        given(productService.getProductWithPessimisticLock(request.productId())).willReturn(product);

        OrderResponse response = orderService.createDirectOrder(memberId, request);

        assertThat(response.orderNumber()).isNotNull();
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(response.totalAmount()).isEqualTo(20_000L);
        assertThat(response.orderItems()).hasSize(1);
        assertThat(response.orderItems().get(0).productName()).isEqualTo("product");
        assertThat(response.orderItems().get(0).productPrice()).isEqualTo(10_000L);
        assertThat(response.orderItems().get(0).quantity()).isEqualTo(2L);
        assertThat(response.orderItems().get(0).totalPrice()).isEqualTo(20_000L);
        assertThat(product.getStock()).isEqualTo(8);
        assertThat(product.getSalesCount()).isEqualTo(2);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("존재하지 않는 회원이 바로 주문을 요청하면 MEMBER_NOT_FOUND 예외를 던진다")
    void given_nonExistentMember_whenCreateDirectOrder_thenThrowMemberNotFound() {
        Long memberId = 999L;
        DirectOrderRequest request = new DirectOrderRequest(1L, 2);
        given(memberService.getMember(memberId))
                .willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 바로 주문을 요청하면 PRODUCT_NOT_FOUND 예외를 던진다")
    void given_nonExistentProduct_whenCreateDirectOrder_thenThrowProductNotFound() {
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(999L, 2);
        given(memberService.getMember(memberId)).willReturn(createMember(memberId));
        given(productService.getProductWithPessimisticLock(request.productId()))
                .willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("상품 재고가 부족하면 OUT_OF_STOCK 예외를 던진다")
    void given_insufficientStock_whenCreateDirectOrder_thenThrowOutOfStock() {
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(1L, 5);
        Product product = createProduct(1L, "product", 10_000, 3, ProductStatus.ON_SALE);
        given(memberService.getMember(memberId)).willReturn(createMember(memberId));
        given(productService.getProductWithPessimisticLock(request.productId())).willReturn(product);

        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("판매 중이 아닌 상품을 바로 주문하면 NOT_ORDERABLE_PRODUCT 예외를 던진다")
    void given_notOnSaleProduct_whenCreateDirectOrder_thenThrowNotOrderableProduct() {
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(1L, 2);
        Product product = createProduct(1L, "product", 10_000, 10, ProductStatus.STOPPED);
        given(memberService.getMember(memberId)).willReturn(createMember(memberId));
        given(productService.getProductWithPessimisticLock(request.productId())).willReturn(product);

        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ORDERABLE_PRODUCT);
    }

    @Test
    @DisplayName("회원의 장바구니 상품들이 주문 가능하면 장바구니 주문 생성에 성공한다")
    void given_validCartItems_whenCreateCartOrder_thenSuccess() {
        Long memberId = 1L;
        CartOrderRequest request = new CartOrderRequest(List.of(10L, 20L));
        Member member = createMember(memberId);
        Product firstProduct = createProduct(100L, "keyboard", 10_000, 10, ProductStatus.ON_SALE);
        Product secondProduct = createProduct(200L, "mouse", 5_000, 10, ProductStatus.ON_SALE);
        Cart firstCart = createCart(10L, member, firstProduct, 2);
        Cart secondCart = createCart(20L, member, secondProduct, 1);

        given(memberService.getMember(memberId)).willReturn(member);
        given(cartService.getOrderCartItemsWithoutProduct(memberId, request.cartIds()))
                .willReturn(List.of(firstCart, secondCart));
        given(productService.getProductWithPessimisticLock(firstProduct.getId())).willReturn(firstProduct);
        given(productService.getProductWithPessimisticLock(secondProduct.getId())).willReturn(secondProduct);

        OrderResponse response = orderService.createCartOrder(memberId, request);

        assertThat(response.orderNumber()).isNotNull();
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(response.totalAmount()).isEqualTo(25_000L);
        assertThat(response.orderItems()).hasSize(2);
        assertThat(response.orderItems().get(0).productName()).isEqualTo("keyboard");
        assertThat(response.orderItems().get(0).productPrice()).isEqualTo(10_000L);
        assertThat(response.orderItems().get(0).quantity()).isEqualTo(2L);
        assertThat(response.orderItems().get(0).totalPrice()).isEqualTo(20_000L);
        assertThat(firstProduct.getStock()).isEqualTo(8);
        assertThat(firstProduct.getSalesCount()).isEqualTo(2);
        assertThat(secondProduct.getStock()).isEqualTo(9);
        assertThat(secondProduct.getSalesCount()).isEqualTo(1);
        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(any());
        verify(cartService).deleteOrderCartItems(List.of(firstCart, secondCart));
    }

    @Test
    @DisplayName("장바구니 주문 대상 상품 재고가 부족하면 OUT_OF_STOCK 예외를 던진다")
    void given_insufficientStockCartItem_whenCreateCartOrder_thenThrowOutOfStock() {
        Long memberId = 1L;
        CartOrderRequest request = new CartOrderRequest(List.of(10L));
        Member member = createMember(memberId);
        Product product = createProduct(100L, "keyboard", 10_000, 1, ProductStatus.ON_SALE);
        Cart cart = createCart(10L, member, product, 2);
        given(memberService.getMember(memberId)).willReturn(member);
        given(cartService.getOrderCartItemsWithoutProduct(memberId, request.cartIds())).willReturn(List.of(cart));
        given(productService.getProductWithPessimisticLock(product.getId())).willReturn(product);

        assertThatThrownBy(() -> orderService.createCartOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("장바구니 주문 대상 상품이 판매 중이 아니면 NOT_ORDERABLE_PRODUCT 예외를 던진다")
    void given_notOnSaleCartItem_whenCreateCartOrder_thenThrowNotOrderableProduct() {
        Long memberId = 1L;
        CartOrderRequest request = new CartOrderRequest(List.of(10L));
        Member member = createMember(memberId);
        Product product = createProduct(100L, "keyboard", 10_000, 10, ProductStatus.STOPPED);
        Cart cart = createCart(10L, member, product, 1);
        given(memberService.getMember(memberId)).willReturn(member);
        given(cartService.getOrderCartItemsWithoutProduct(memberId, request.cartIds())).willReturn(List.of(cart));
        given(productService.getProductWithPessimisticLock(product.getId())).willReturn(product);

        assertThatThrownBy(() -> orderService.createCartOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ORDERABLE_PRODUCT);
    }

    @Test
    @DisplayName("cartIds가 없으면 회원의 전체 장바구니 상품으로 주문 미리보기를 반환한다")
    void given_noCartIds_whenPreviewOrder_thenReturnAllCartItems() {
        Long memberId = 1L;
        Cart firstCart = createCart(10L, createMember(memberId), createProduct(100L, "keyboard", 10_000, 10, ProductStatus.ON_SALE), 2);
        Cart secondCart = createCart(20L, createMember(memberId), createProduct(200L, "mouse", 5_000, 10, ProductStatus.ON_SALE), 1);
        given(cartService.getOrderPreviewCartItems(memberId, null))
                .willReturn(List.of(firstCart, secondCart));

        OrderPreviewResponse response = orderService.previewOrder(memberId, null);

        assertThat(response.orderItems()).hasSize(2);
        assertThat(response.orderItems().get(0).cartId()).isEqualTo(10L);
        assertThat(response.orderItems().get(0).productId()).isEqualTo(100L);
        assertThat(response.orderItems().get(0).productName()).isEqualTo("keyboard");
        assertThat(response.orderItems().get(0).productPrice()).isEqualTo(10_000L);
        assertThat(response.orderItems().get(0).quantity()).isEqualTo(2);
        assertThat(response.orderItems().get(0).productTotalAmount()).isEqualTo(20_000L);
        assertThat(response.totalOrderAmount()).isEqualTo(25_000L);
    }

    @Test
    @DisplayName("cartIds가 있으면 선택된 장바구니 상품으로 주문 미리보기를 반환한다")
    void given_cartIds_whenPreviewOrder_thenReturnSelectedCartItems() {
        Long memberId = 1L;
        List<Long> cartIds = List.of(10L, 10L, 20L);
        Cart firstCart = createCart(10L, createMember(memberId), createProduct(100L, "keyboard", 10_000, 10, ProductStatus.ON_SALE), 1);
        Cart secondCart = createCart(20L, createMember(memberId), createProduct(200L, "mouse", 5_000, 10, ProductStatus.ON_SALE), 1);
        given(cartService.getOrderPreviewCartItems(memberId, cartIds))
                .willReturn(List.of(firstCart, secondCart));

        OrderPreviewResponse response = orderService.previewOrder(memberId, cartIds);

        assertThat(response.orderItems()).hasSize(2);
        assertThat(response.orderItems().get(0).cartId()).isEqualTo(10L);
        assertThat(response.orderItems().get(0).productId()).isEqualTo(100L);
        assertThat(response.totalOrderAmount()).isEqualTo(15_000L);
    }

    @Test
    @DisplayName("유효하지 않은 장바구니 ID가 있으면 CART_ITEM_NOT_FOUND 예외를 던진다")
    void given_invalidCartId_whenPreviewOrder_thenThrowCartItemNotFound() {
        Long memberId = 1L;
        List<Long> cartIds = List.of(10L, 20L);
        given(cartService.getOrderPreviewCartItems(memberId, cartIds))
                .willThrow(new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        assertThatThrownBy(() -> orderService.previewOrder(memberId, cartIds))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);
    }

    @Test
    @DisplayName("주문 미리보기 대상 장바구니가 비어 있으면 EMPTY_ORDER_PREVIEW 예외를 던진다")
    void given_emptyCart_whenPreviewOrder_thenThrowEmptyOrderPreview() {
        Long memberId = 1L;
        given(cartService.getOrderPreviewCartItems(memberId, null)).willReturn(List.of());

        assertThatThrownBy(() -> orderService.previewOrder(memberId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMPTY_ORDER_PREVIEW);
    }

    @Test
    @DisplayName("판매 중이 아닌 상품이 장바구니에 있으면 NOT_ORDERABLE_PRODUCT 예외를 던진다")
    void given_notOnSaleProduct_whenPreviewOrder_thenThrowNotOrderableProduct() {
        Long memberId = 1L;
        Cart cart = createCart(10L, createMember(memberId), createProduct(100L, "keyboard", 10_000, 10, ProductStatus.STOPPED), 1);
        given(cartService.getOrderPreviewCartItems(memberId, null)).willReturn(List.of(cart));

        assertThatThrownBy(() -> orderService.previewOrder(memberId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ORDERABLE_PRODUCT);
    }

    @Test
    @DisplayName("장바구니 상품 재고가 부족하면 OUT_OF_STOCK 예외를 던진다")
    void given_insufficientStock_whenPreviewOrder_thenThrowOutOfStock() {
        Long memberId = 1L;
        Cart cart = createCart(10L, createMember(memberId), createProduct(100L, "keyboard", 10_000, 1, ProductStatus.ON_SALE), 2);
        given(cartService.getOrderPreviewCartItems(memberId, null)).willReturn(List.of(cart));

        assertThatThrownBy(() -> orderService.previewOrder(memberId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("주문 소유자가 취소를 요청하면 주문 상태와 취소 시각을 변경한다")
    void given_orderOwner_whenCancelOrder_thenSuccess() {
        Long memberId = 1L;
        Long orderId = 1L;
        Member member = createMember(memberId);
        Order order = Order.create(member, "order-123", 20000L, 20000L);
        ReflectionTestUtils.setField(order, "id", orderId);
        given(orderRepository.findByIdAndMemberId(orderId, memberId)).willReturn(Optional.of(order));

        orderService.cancelOrder(memberId, orderId);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCanceledAt()).isEqualTo(LocalDateTime.now(fixedClock));
    }

    @Test
    @DisplayName("존재하지 않거나 소유자가 다른 주문을 취소하려고 하면 ORDER_NOT_FOUND 예외를 던진다")
    void given_notOwnedOrder_whenCancelOrder_thenThrowOrderNotFound() {
        Long memberId = 1L;
        Long orderId = 999L;
        given(orderRepository.findByIdAndMemberId(orderId, memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(memberId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 취소된 주문을 다시 취소하려고 하면 INVALID_ORDER_STATUS 예외를 던진다")
    void given_cancelledOrder_whenCancelAgain_thenThrowInvalidOrderStatus() {
        Long memberId = 1L;
        Long orderId = 1L;
        Member member = createMember(memberId);
        Order order = Order.create(member, "order-123", 20000L, 20000L);
        ReflectionTestUtils.setField(order, "id", orderId);
        order.cancel(LocalDateTime.now(fixedClock));
        given(orderRepository.findByIdAndMemberId(orderId, memberId)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(memberId, orderId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ORDER_STATUS);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCanceledAt()).isNotNull();
        assertThat(order.getPaidAt()).isNull();
    }

    @Test
    @DisplayName("회원의 주문 내역이 존재하면 주문 요약만 담은 페이징 응답을 반환한다")
    void given_validMemberAndOrders_whenGetOrderHistory_thenReturnPagedOrderHistoryResponse() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        Member member = Member.create("test@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);

        Order order = Order.create(member, "ORD-123", 20000L, 20000L);
        ReflectionTestUtils.setField(order, "id", 100L);

        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

        given(orderRepository.findOrderHistoryByMemberId(eq(memberId), any(OrderSearchCondition.class), eq(pageable))).willReturn(orderPage);

        // when
        PageResponse<OrderHistoryResponse> result = orderService.getOrderHistory(memberId, new OrderSearchCondition(null, null, null, null), pageable);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).orderId()).isEqualTo(100L);
        assertThat(result.content().get(0).orderNumber()).isEqualTo("ORD-123");
        assertThat(result.content().get(0).totalAmount()).isEqualTo(20000L);
        assertThat(result.content().get(0).orderStatus()).isEqualTo("PENDING");
        verify(orderItemRepository, never()).findByOrderIdIn(any());
    }

    @Test
    @DisplayName("주문 내역이 없는 회원이면 빈 페이지 응답을 반환하고 추가 쿼리를 수행하지 않는다")
    void given_noOrders_whenGetOrderHistory_thenReturnEmptyPageResponse() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(orderRepository.findOrderHistoryByMemberId(eq(memberId), any(OrderSearchCondition.class), eq(pageable))).willReturn(emptyPage);

        // when
        PageResponse<OrderHistoryResponse> result = orderService.getOrderHistory(memberId, new OrderSearchCondition(null, null, null, null), pageable);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
        verify(orderItemRepository, never()).findByOrderIdIn(any());
    }

    @Test
    @DisplayName("관리자 전체 주문 검색 결과를 관리자 응답 DTO로 변환한다")
    void given_adminOrderSearchCondition_whenGetAdminOrders_thenReturnPagedAdminOrderResponse() {
        // given
        Long memberId = 1L;
        Pageable pageable = PageRequest.of(0, 20);

        Member member = Member.create("member@example.com", "password", "member", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);

        Order order = Order.create(member, "TARGET-ORDER-000002", 20000L, 20000L);
        ReflectionTestUtils.setField(order, "id", 100L);
        ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.PAID);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.of(2026, 6, 30, 12, 0));
        ReflectionTestUtils.setField(order, "paidAt", LocalDateTime.of(2026, 6, 30, 12, 10));

        AdminOrderSearchCondition condition = new AdminOrderSearchCondition(
                null,
                null,
                OrderStatus.PAID,
                10000L,
                500000L,
                "TARGET-ORDER-000002", // orderNumber: 관리자 주문번호 검색은 정확 검색으로 처리한다.
                null,                  // productName: 상품명 검색 조건은 사용하지 않는다.
                null                   // memberId: 특정 회원 검색 조건은 사용하지 않는다.
        );

        Page<Order> orderPage = new PageImpl<>(List.of(order), pageable, 1);

        given(orderRepository.findAdminOrders(eq(condition), eq(pageable))).willReturn(orderPage);

        // when
        PageResponse<AdminOrderSearchResponse> result = orderService.getAdminOrders(condition, pageable);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).orderId()).isEqualTo(100L);
        assertThat(result.content().get(0).orderNumber()).isEqualTo("TARGET-ORDER-000002");
        assertThat(result.content().get(0).memberId()).isEqualTo(memberId);
        assertThat(result.content().get(0).totalAmount()).isEqualTo(20000L);
        assertThat(result.content().get(0).pgAmount()).isEqualTo(20000L);
        assertThat(result.content().get(0).orderStatus()).isEqualTo("PAID");
        assertThat(result.content().get(0).paidAt()).isEqualTo(LocalDateTime.of(2026, 6, 30, 12, 10));

        // 관리자 주문 목록 조회는 주문 요약 정보만 반환한다.
        // 주문 상품 목록은 상세 조회에서만 가져오므로 추가 order_items 조회가 없어야 한다.
        verify(orderItemRepository, never()).findByOrderIdIn(any());
    }

    @Test
    @DisplayName("관리자 주문 상세 조회는 주문 기본 정보와 주문 상품 목록을 반환한다")
    void givenOrderId_whenGetAdminOrderDetail_thenReturnOrderWithItems() {
        // given
        Long orderId = 100L;
        Long memberId = 1L;

        Member member = createMember(memberId);
        Order order = Order.create(member, "TARGET-ORDER-000002", 30000L, 30000L);
        ReflectionTestUtils.setField(order, "id", orderId);
        ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.PAID);
        ReflectionTestUtils.setField(order, "createdAt", LocalDateTime.of(2026, 6, 30, 12, 0));
        ReflectionTestUtils.setField(order, "paidAt", LocalDateTime.of(2026, 6, 30, 12, 10));

        Product product = createProduct(10L, "게이밍 노트북", 10000, 10, ProductStatus.ON_SALE);
        OrderItem orderItem = OrderItem.create(order, product, "게이밍 노트북", 10000L, 2L, 20000L, null, null);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));
        given(orderItemRepository.findByOrderIdIn(List.of(orderId))).willReturn(List.of(orderItem));

        // when
        AdminOrderDetailResponse result = orderService.getAdminOrderDetail(orderId);

        // then
        assertThat(result.orderId()).isEqualTo(orderId);
        assertThat(result.orderNumber()).isEqualTo("TARGET-ORDER-000002");
        assertThat(result.memberId()).isEqualTo(memberId);
        assertThat(result.totalAmount()).isEqualTo(30000L);
        assertThat(result.pgAmount()).isEqualTo(30000L);
        assertThat(result.orderStatus()).isEqualTo("PAID");
        assertThat(result.createdAt()).isEqualTo(LocalDateTime.of(2026, 6, 30, 12, 0));
        assertThat(result.paidAt()).isEqualTo(LocalDateTime.of(2026, 6, 30, 12, 10));
        assertThat(result.orderItems()).hasSize(1);
        assertThat(result.orderItems().get(0).productName()).isEqualTo("게이밍 노트북");
        assertThat(result.orderItems().get(0).quantity()).isEqualTo(2L);
    }

    @Test
    @DisplayName("관리자 주문 상세 조회 대상 주문이 없으면 ORDER_NOT_FOUND 예외를 던진다")
    void givenMissingOrderId_whenGetAdminOrderDetail_thenThrowOrderNotFound() {
        // given
        Long orderId = 999L;
        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> orderService.getAdminOrderDetail(orderId))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.ORDER_NOT_FOUND.getMessage());
        verify(orderItemRepository, never()).findByOrderIdIn(any());
    }

    private Cart createCart(Long cartId, Member member, Product product, int quantity) {
        Cart cart = Cart.create(member, product, quantity);
        ReflectionTestUtils.setField(cart, "id", cartId);

        return cart;
    }

    private Product createProduct(Long productId, String name, int price, int stock, ProductStatus status) {
        Product product = Product.create(name, price, stock, status, "description", null);
        ReflectionTestUtils.setField(product, "id", productId);

        return product;
    }

    private Member createMember(Long memberId) {
        Member member = Member.create("member@example.com", "password", "member", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);

        return member;
    }
}
