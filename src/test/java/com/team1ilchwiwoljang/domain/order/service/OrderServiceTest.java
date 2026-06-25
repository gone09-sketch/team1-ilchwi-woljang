package com.team1ilchwiwoljang.domain.order.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.service.CartService;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.order.dto.request.CartOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import com.team1ilchwiwoljang.domain.order.repository.OrderItemRepository;
import com.team1ilchwiwoljang.domain.order.repository.OrderRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductService productService;

    @Mock
    private MemberService memberService;

    @Mock
    private CartService cartService;

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

        given(memberService.getMember(memberId)).willReturn(member);
        given(productService.getProduct(request.productId())).willReturn(product);

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

        given(memberService.getMember(memberId))
                .willThrow(new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

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

        given(memberService.getMember(memberId)).willReturn(member);
        given(productService.getProduct(request.productId()))
                .willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

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

        given(memberService.getMember(memberId)).willReturn(member);
        given(productService.getProduct(request.productId())).willReturn(product);

        // when & then
        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("판매 중이 아닌 상품을 바로 주문하면 NOT_ORDERABLE_PRODUCT 예외를 던진다")
    void given_notOnSaleProduct_whenCreateDirectOrder_thenThrowNotOrderableProduct() {
        // given
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(1L, 1);

        Member member = Member.create("test@example.com", "password", "tester", "010-1234-5678");
        Product product = Product.create("keyboard", 10000, 10, ProductStatus.STOPPED, "description", null);

        given(memberService.getMember(memberId)).willReturn(member);
        given(productService.getProduct(request.productId())).willReturn(product);

        // when & then
        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ORDERABLE_PRODUCT);
    }

    @Test
    @DisplayName("회원의 장바구니 상품들이 주문 가능하면 장바구니 주문 생성에 성공한다")
    void given_validCartItems_whenCreateCartOrder_thenSuccess() {
        // given
        Long memberId = 1L;
        CartOrderRequest request = new CartOrderRequest(List.of(10L, 20L));

        Member member = Member.create("test@example.com", "password", "tester", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);

        Product firstProduct = Product.create("keyboard", 10000, 10, ProductStatus.ON_SALE, "description", null);
        Product secondProduct = Product.create("mouse", 5000, 10, ProductStatus.ON_SALE, "description", null);
        Cart firstCart = Cart.create(member, firstProduct, 2);
        Cart secondCart = Cart.create(member, secondProduct, 1);

        given(memberService.getMember(memberId)).willReturn(member);
        given(cartService.getOrderCartItems(memberId, request.cartIds()))
                .willReturn(List.of(firstCart, secondCart));

        // when
        OrderResponse response = orderService.createCartOrder(memberId, request);

        // then
        assertThat(response.orderNumber()).isNotNull();
        assertThat(response.orderStatus()).isEqualTo(OrderStatus.PENDING.name());
        assertThat(response.totalAmount()).isEqualTo(25000L);
        assertThat(response.orderItems()).hasSize(2);
        assertThat(response.orderItems().get(0).productName()).isEqualTo("keyboard");
        assertThat(response.orderItems().get(0).productPrice()).isEqualTo(10000L);
        assertThat(response.orderItems().get(0).quantity()).isEqualTo(2L);
        assertThat(response.orderItems().get(0).totalPrice()).isEqualTo(20000L);
        assertThat(firstProduct.getStock()).isEqualTo(8);
        assertThat(secondProduct.getStock()).isEqualTo(9);

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).saveAll(any());
        verify(cartService).deleteOrderCartItems(List.of(firstCart, secondCart));
    }

    @Test
    @DisplayName("장바구니 주문 대상 상품 재고가 부족하면 OUT_OF_STOCK 예외를 던진다")
    void given_insufficientStockCartItem_whenCreateCartOrder_thenThrowOutOfStock() {
        // given
        Long memberId = 1L;
        CartOrderRequest request = new CartOrderRequest(List.of(10L));

        Member member = Member.create("test@example.com", "password", "tester", "010-1234-5678");
        Product product = Product.create("keyboard", 10000, 1, ProductStatus.ON_SALE, "description", null);
        Cart cart = Cart.create(member, product, 2);

        given(memberService.getMember(memberId)).willReturn(member);
        given(cartService.getOrderCartItems(memberId, request.cartIds()))
                .willReturn(List.of(cart));

        // when & then
        assertThatThrownBy(() -> orderService.createCartOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
    }

    @Test
    @DisplayName("장바구니 주문 대상 상품이 판매 중이 아니면 NOT_ORDERABLE_PRODUCT 예외를 던진다")
    void given_notOnSaleCartItem_whenCreateCartOrder_thenThrowNotOrderableProduct() {
        // given
        Long memberId = 1L;
        CartOrderRequest request = new CartOrderRequest(List.of(10L));

        Member member = Member.create("test@example.com", "password", "tester", "010-1234-5678");
        Product product = Product.create("keyboard", 10000, 10, ProductStatus.STOPPED, "description", null);
        Cart cart = Cart.create(member, product, 1);

        given(memberService.getMember(memberId)).willReturn(member);
        given(cartService.getOrderCartItems(memberId, request.cartIds()))
                .willReturn(List.of(cart));

        // when & then
        assertThatThrownBy(() -> orderService.createCartOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ORDERABLE_PRODUCT);
    }
}
