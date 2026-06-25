package com.team1ilchwiwoljang.domain.order.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderPreviewRequest;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    @DisplayName("판매 중인 상품과 충분한 재고가 있으면 바로 구매 미리보기에 성공한다")
    void given_validRequest_whenPreviewDirectOrder_thenSuccess() {
        // given
        DirectOrderPreviewRequest request = new DirectOrderPreviewRequest(1L, 2);

        Product product = Product.create("상품명", 10000, 10, ProductStatus.ON_SALE, "상품 설명", null);
        ReflectionTestUtils.setField(product, "id", 1L);

        given(productService.getProduct(request.productId())).willReturn(product);

        // when
        OrderPreviewResponse response = orderService.previewDirectOrder(request);

        // then
        assertThat(response.totalAmount()).isEqualTo(20000L);
        assertThat(response.orderItems()).hasSize(1);
        assertThat(response.orderItems().get(0).productName()).isEqualTo("상품명");
        assertThat(response.orderItems().get(0).productPrice()).isEqualTo(10000L);
        assertThat(response.orderItems().get(0).quantity()).isEqualTo(2L);
        assertThat(response.orderItems().get(0).productTotalAmount()).isEqualTo(20000L);

        assertThat(product.getStock()).isEqualTo(10); // 미리보기에서는 실제 재고를 차감하지 않는다.
    }

    @Test
    @DisplayName("존재하지 않는 상품 ID로 바로 구매 미리보기를 요청하면 PRODUCT_NOT_FOUND 예외를 던진다")
    void given_nonExistentProduct_whenPreviewDirectOrder_thenThrowProductNotFound() {
        // given
        DirectOrderPreviewRequest request = new DirectOrderPreviewRequest(999L, 2);

        given(productService.getProduct(request.productId()))
                .willThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> orderService.previewDirectOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    @DisplayName("판매 중이 아닌 상품으로 바로 구매 미리보기를 요청하면 NOT_ORDERABLE_PRODUCT 예외를 던진다")
    void given_notOrderableProduct_whenPreviewDirectOrder_thenThrowNotOrderableProduct() {
        // given
        DirectOrderPreviewRequest request = new DirectOrderPreviewRequest(1L, 2);

        Product product = Product.create("상품명", 10000, 10, ProductStatus.STOPPED, "상품 설명", null);
        ReflectionTestUtils.setField(product, "id", 1L);

        given(productService.getProduct(request.productId())).willReturn(product);

        // when & then
        assertThatThrownBy(() -> orderService.previewDirectOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ORDERABLE_PRODUCT);
    }

    @Test
    @DisplayName("상품 재고가 미리보기 수량보다 부족하면 OUT_OF_STOCK 예외를 던진다")
    void given_insufficientStock_whenPreviewDirectOrder_thenThrowOutOfStock() {
        // given
        DirectOrderPreviewRequest request = new DirectOrderPreviewRequest(1L, 5);

        Product product = Product.create("상품명", 10000, 3, ProductStatus.ON_SALE, "상품 설명", null);
        ReflectionTestUtils.setField(product, "id", 1L);

        given(productService.getProduct(request.productId())).willReturn(product);

        // when & then
        assertThatThrownBy(() -> orderService.previewDirectOrder(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OUT_OF_STOCK);
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
        assertThat(response.orderItems().get(0).productTotalAmount()).isEqualTo(20000L);

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
}
