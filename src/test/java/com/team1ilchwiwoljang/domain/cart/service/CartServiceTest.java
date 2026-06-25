package com.team1ilchwiwoljang.domain.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartItemResponse;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartResponse;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.repository.CartRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("장바구니 목록을 조회하면 상품 목록과 총 금액을 반환한다")
    void getCartReturnsItemsAndTotalPrice() {
        Long memberId = 1L;
        Member member = createMember();
        Product product = createProduct("keyboard", 1_000, 10, ProductStatus.ON_SALE);
        Cart cart = Cart.create(member, product, 3);
        given(cartRepository.findAllByMemberIdWithProduct(memberId))
                .willReturn(List.of(cart));

        CartResponse response = cartService.getCart(memberId);

        assertThat(response.cartTotalPrice()).isEqualTo(3_000L);
        assertThat(response.items()).hasSize(1);

        CartItemResponse item = response.items().get(0);
        assertThat(item.productName()).isEqualTo("keyboard");
        assertThat(item.productPrice()).isEqualTo(1_000);
        assertThat(item.quantity()).isEqualTo(3);
        assertThat(item.itemTotalPrice()).isEqualTo(3_000L);
        assertThat(item.stock()).isEqualTo(10);
        assertThat(item.productStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(item.orderable()).isTrue();
    }

    @Test
    @DisplayName("장바구니 상품 수량이 재고보다 많으면 주문 가능 여부가 false다")
    void getCartReturnsNotOrderableWhenStockIsLessThanQuantity() {
        Long memberId = 1L;
        Member member = createMember();
        Product product = createProduct("keyboard", 1_000, 2, ProductStatus.ON_SALE);
        Cart cart = Cart.create(member, product, 3);
        given(cartRepository.findAllByMemberIdWithProduct(memberId))
                .willReturn(List.of(cart));

        CartResponse response = cartService.getCart(memberId);

        assertThat(response.items().get(0).orderable()).isFalse();
    }

    @Test
    @DisplayName("판매 중이 아닌 상품은 장바구니 조회 응답에서 주문 가능 여부가 false다")
    void getCartReturnsNotOrderableWhenProductIsNotOnSale() {
        Long memberId = 1L;
        Member member = createMember();
        Product outOfStockProduct = createProduct("out-of-stock", 1_000, 10, ProductStatus.OUT_OF_STOCK);
        Product stoppedProduct = createProduct("stopped", 2_000, 10, ProductStatus.STOPPED);
        given(cartRepository.findAllByMemberIdWithProduct(memberId))
                .willReturn(List.of(
                        Cart.create(member, outOfStockProduct, 1),
                        Cart.create(member, stoppedProduct, 1)
                ));

        CartResponse response = cartService.getCart(memberId);

        assertThat(response.items())
                .extracting(CartItemResponse::orderable)
                .containsExactly(false, false);
    }

    @Test
    @DisplayName("장바구니가 비어 있으면 빈 목록과 0원을 반환한다")
    void getCartReturnsEmptyItemsAndZeroTotalPrice() {
        Long memberId = 1L;
        given(cartRepository.findAllByMemberIdWithProduct(memberId))
                .willReturn(List.of());

        CartResponse response = cartService.getCart(memberId);

        assertThat(response.items()).isEmpty();
        assertThat(response.cartTotalPrice()).isZero();
    }

    @Test
    @DisplayName("주문 대상 장바구니 ID를 중복 제거한 뒤 선택된 장바구니 상품을 조회한다")
    void getOrderCartItemsReturnsSelectedCartItems() {
        Long memberId = 1L;
        List<Long> cartIds = Arrays.asList(10L, 10L, null, 20L);
        List<Long> selectedCartIds = List.of(10L, 20L);
        Cart firstCart = Cart.create(createMember(), createProduct("keyboard", 10_000, 10, ProductStatus.ON_SALE), 1);
        Cart secondCart = Cart.create(createMember(), createProduct("mouse", 5_000, 10, ProductStatus.ON_SALE), 1);

        given(cartRepository.findAllByMemberIdAndIdInWithProduct(memberId, selectedCartIds))
                .willReturn(List.of(firstCart, secondCart));

        List<Cart> cartItems = cartService.getOrderCartItems(memberId, cartIds);

        assertThat(cartItems).containsExactly(firstCart, secondCart);
        verify(cartRepository).findAllByMemberIdAndIdInWithProduct(memberId, selectedCartIds);
    }

    @Test
    @DisplayName("주문 대상 장바구니 ID 목록이 비어 있으면 EMPTY_CART_ORDER 예외가 발생한다")
    void getOrderCartItemsThrowsWhenCartIdsAreEmpty() {
        Long memberId = 1L;

        assertThatThrownBy(() -> cartService.getOrderCartItems(memberId, List.of()))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMPTY_CART_ORDER));
    }

    @Test
    @DisplayName("주문 대상 장바구니 ID가 모두 null이면 CART_ITEM_NOT_FOUND 예외가 발생한다")
    void getOrderCartItemsThrowsWhenOnlyNullCartIdsAreProvided() {
        Long memberId = 1L;
        List<Long> cartIds = Arrays.asList(null, null);

        assertThatThrownBy(() -> cartService.getOrderCartItems(memberId, cartIds))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    @Test
    @DisplayName("존재하지 않거나 다른 회원의 장바구니 상품이 포함되면 CART_ITEM_NOT_FOUND 예외가 발생한다")
    void getOrderCartItemsThrowsWhenSelectedCartItemDoesNotExistOrBelongsToAnotherMember() {
        Long memberId = 1L;
        List<Long> cartIds = List.of(10L, 20L);
        Cart cart = Cart.create(createMember(), createProduct("keyboard", 10_000, 10, ProductStatus.ON_SALE), 1);

        given(cartRepository.findAllByMemberIdAndIdInWithProduct(memberId, cartIds))
                .willReturn(List.of(cart));

        assertThatThrownBy(() -> cartService.getOrderCartItems(memberId, cartIds))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND));
    }

    @Test
    @DisplayName("주문 완료된 장바구니 상품 목록을 삭제한다")
    void deleteOrderCartItemsDeletesRequestedCartItems() {
        Cart firstCart = Cart.create(createMember(), createProduct("keyboard", 10_000, 10, ProductStatus.ON_SALE), 1);
        Cart secondCart = Cart.create(createMember(), createProduct("mouse", 5_000, 10, ProductStatus.ON_SALE), 1);
        List<Cart> cartItems = List.of(firstCart, secondCart);

        cartService.deleteOrderCartItems(cartItems);

        verify(cartRepository).deleteAll(cartItems);
    }

    private Member createMember() {
        return Member.create("member@example.com", "password", "member", "010-1234-5678");
    }

    private Product createProduct(String name, int price, int stock, ProductStatus status) {
        return Product.create(name, price, stock, status, "description", null);
    }
}
