package com.team1ilchwiwoljang.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.service.CartService;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
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
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

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

    @InjectMocks
    private OrderService orderService;

    @Test
    void given_validRequest_whenCreateDirectOrder_thenSuccess() {
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(1L, 2);

        Member member = createMember();
        ReflectionTestUtils.setField(member, "id", memberId);

        Product product = createProduct("product", 10_000, 10, ProductStatus.ON_SALE);
        ReflectionTestUtils.setField(product, "id", 1L);

        given(memberService.getMember(memberId)).willReturn(member);
        given(productService.getProduct(request.productId())).willReturn(product);

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

        verify(orderRepository).save(any(Order.class));
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
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
    void given_nonExistentProduct_whenCreateDirectOrder_thenThrowProductNotFound() {
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(999L, 2);

        given(memberService.getMember(memberId)).willReturn(createMember());
        given(productService.getProduct(request.productId()))
                .willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void given_insufficientStock_whenCreateDirectOrder_thenThrowOutOfStock() {
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(1L, 5);
        Product product = createProduct("product", 10_000, 3, ProductStatus.ON_SALE);

        given(memberService.getMember(memberId)).willReturn(createMember());
        given(productService.getProduct(request.productId())).willReturn(product);

        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
    }

    @Test
    void given_notOnSaleProduct_whenCreateDirectOrder_thenThrowNotOrderableProduct() {
        Long memberId = 1L;
        DirectOrderRequest request = new DirectOrderRequest(1L, 2);
        Product product = createProduct("product", 10_000, 10, ProductStatus.STOPPED);

        given(memberService.getMember(memberId)).willReturn(createMember());
        given(productService.getProduct(request.productId())).willReturn(product);

        assertThatThrownBy(() -> orderService.createDirectOrder(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ORDERABLE_PRODUCT);
    }

    @Test
    void given_noCartIds_whenPreviewOrder_thenReturnAllCartItems() {
        Long memberId = 1L;
        Cart firstCart = createCart(createProduct("keyboard", 10_000, 10, ProductStatus.ON_SALE), 2);
        Cart secondCart = createCart(createProduct("mouse", 5_000, 10, ProductStatus.ON_SALE), 1);

        given(cartService.getOrderPreviewCartItems(memberId, null))
                .willReturn(List.of(firstCart, secondCart));

        OrderPreviewResponse response = orderService.previewOrder(memberId, null);

        assertThat(response.orderItems()).hasSize(2);
        assertThat(response.orderItems().get(0).productName()).isEqualTo("keyboard");
        assertThat(response.orderItems().get(0).productPrice()).isEqualTo(10_000L);
        assertThat(response.orderItems().get(0).quantity()).isEqualTo(2);
        assertThat(response.orderItems().get(0).productTotalAmount()).isEqualTo(20_000L);
        assertThat(response.totalOrderAmount()).isEqualTo(25_000L);
        assertThat(response.finalPaymentAmount()).isEqualTo(25_000L);
    }

    @Test
    void given_cartIds_whenPreviewOrder_thenReturnSelectedCartItems() {
        Long memberId = 1L;
        List<Long> cartIds = List.of(10L, 10L, 20L);
        Cart firstCart = createCart(createProduct("keyboard", 10_000, 10, ProductStatus.ON_SALE), 1);
        Cart secondCart = createCart(createProduct("mouse", 5_000, 10, ProductStatus.ON_SALE), 1);

        given(cartService.getOrderPreviewCartItems(memberId, cartIds))
                .willReturn(List.of(firstCart, secondCart));

        OrderPreviewResponse response = orderService.previewOrder(memberId, cartIds);

        assertThat(response.orderItems()).hasSize(2);
        assertThat(response.totalOrderAmount()).isEqualTo(15_000L);
        assertThat(response.finalPaymentAmount()).isEqualTo(15_000L);
    }

    @Test
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
    void given_emptyCart_whenPreviewOrder_thenThrowEmptyOrderPreview() {
        Long memberId = 1L;

        given(cartService.getOrderPreviewCartItems(memberId, null))
                .willReturn(List.of());

        assertThatThrownBy(() -> orderService.previewOrder(memberId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMPTY_ORDER_PREVIEW);
    }

    @Test
    void given_notOnSaleProduct_whenPreviewOrder_thenThrowNotOrderableProduct() {
        Long memberId = 1L;
        Cart cart = createCart(createProduct("keyboard", 10_000, 10, ProductStatus.STOPPED), 1);

        given(cartService.getOrderPreviewCartItems(memberId, null))
                .willReturn(List.of(cart));

        assertThatThrownBy(() -> orderService.previewOrder(memberId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ORDERABLE_PRODUCT);
    }

    @Test
    void given_insufficientStock_whenPreviewOrder_thenThrowOutOfStock() {
        Long memberId = 1L;
        Cart cart = createCart(createProduct("keyboard", 10_000, 1, ProductStatus.ON_SALE), 2);

        given(cartService.getOrderPreviewCartItems(memberId, null))
                .willReturn(List.of(cart));

        assertThatThrownBy(() -> orderService.previewOrder(memberId, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
    }

    private Cart createCart(Product product, int quantity) {
        return Cart.create(createMember(), product, quantity);
    }

    private Product createProduct(String name, int price, int stock, ProductStatus status) {
        return Product.create(name, price, stock, status, "description", null);
    }

    private Member createMember() {
        return Member.create("member@example.com", "password", "member", "010-1234-5678");
    }
}
