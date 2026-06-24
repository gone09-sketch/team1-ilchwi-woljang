package com.team1ilchwiwoljang.domain.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

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
    void getCartReturnsItemsAndTotalPrice() {
        Long memberId = 1L;
        Product product = createProduct("product", 1_000, 10, ProductStatus.ON_SALE);
        Cart cart = Cart.create(createMember(), product, 3);
        given(cartRepository.findAllByMemberIdWithProduct(memberId))
                .willReturn(List.of(cart));

        CartResponse response = cartService.getCart(memberId);

        assertThat(response.cartTotalPrice()).isEqualTo(3_000L);
        assertThat(response.items()).hasSize(1);

        CartItemResponse item = response.items().get(0);
        assertThat(item.productName()).isEqualTo("product");
        assertThat(item.productPrice()).isEqualTo(1_000);
        assertThat(item.quantity()).isEqualTo(3);
        assertThat(item.itemTotalPrice()).isEqualTo(3_000L);
        assertThat(item.stock()).isEqualTo(10);
        assertThat(item.productStatus()).isEqualTo(ProductStatus.ON_SALE);
        assertThat(item.orderable()).isTrue();
    }

    @Test
    void getCartReturnsNotOrderableWhenStockIsLessThanQuantity() {
        Long memberId = 1L;
        Product product = createProduct("product", 1_000, 2, ProductStatus.ON_SALE);
        Cart cart = Cart.create(createMember(), product, 3);
        given(cartRepository.findAllByMemberIdWithProduct(memberId))
                .willReturn(List.of(cart));

        CartResponse response = cartService.getCart(memberId);

        assertThat(response.items().get(0).orderable()).isFalse();
    }

    @Test
    void getCartReturnsNotOrderableWhenProductIsNotOnSale() {
        Long memberId = 1L;
        Member member = createMember();
        Product outOfStockProduct = createProduct("out-of-stock product", 1_000, 10, ProductStatus.OUT_OF_STOCK);
        Product stoppedProduct = createProduct("stopped product", 2_000, 10, ProductStatus.STOPPED);
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
    void getCartReturnsEmptyItemsAndZeroTotalPrice() {
        Long memberId = 1L;
        given(cartRepository.findAllByMemberIdWithProduct(memberId))
                .willReturn(List.of());

        CartResponse response = cartService.getCart(memberId);

        assertThat(response.items()).isEmpty();
        assertThat(response.cartTotalPrice()).isZero();
    }

    @Test
    @DisplayName("cartIds가 없으면 회원의 전체 장바구니 상품을 조회한다")
    void getOrderPreviewCartItemsReturnsAllCartItemsWhenCartIdsAreNotProvided() {
        Long memberId = 1L;
        Cart cart = Cart.create(createMember(), createProduct("keyboard", 10_000, 10, ProductStatus.ON_SALE), 1);
        given(cartRepository.findAllByMemberIdWithProduct(memberId))
                .willReturn(List.of(cart));

        List<Cart> cartItems = cartService.getOrderPreviewCartItems(memberId, null);

        assertThat(cartItems).containsExactly(cart);
    }

    @Test
    @DisplayName("cartIds가 있으면 중복을 제거한 뒤 선택된 장바구니 상품만 조회한다")
    void getOrderPreviewCartItemsReturnsSelectedCartItemsWhenCartIdsAreProvided() {
        Long memberId = 1L;
        List<Long> cartIds = List.of(10L, 10L, 20L);
        List<Long> selectedCartIds = List.of(10L, 20L);
        Cart firstCart = Cart.create(createMember(), createProduct("keyboard", 10_000, 10, ProductStatus.ON_SALE), 1);
        Cart secondCart = Cart.create(createMember(), createProduct("mouse", 5_000, 10, ProductStatus.ON_SALE), 1);
        given(cartRepository.findAllByMemberIdAndIdInWithProduct(memberId, selectedCartIds))
                .willReturn(List.of(firstCart, secondCart));

        List<Cart> cartItems = cartService.getOrderPreviewCartItems(memberId, cartIds);

        assertThat(cartItems).containsExactly(firstCart, secondCart);
    }

    @Test
    @DisplayName("선택한 장바구니 상품 중 존재하지 않거나 다른 회원의 상품이 있으면 예외가 발생한다")
    void getOrderPreviewCartItemsThrowsWhenSelectedCartItemDoesNotExistOrBelongsToAnotherMember() {
        Long memberId = 1L;
        List<Long> cartIds = List.of(10L, 20L);
        Cart cart = Cart.create(createMember(), createProduct("keyboard", 10_000, 10, ProductStatus.ON_SALE), 1);
        given(cartRepository.findAllByMemberIdAndIdInWithProduct(memberId, cartIds))
                .willReturn(List.of(cart));

        assertThatThrownBy(() -> cartService.getOrderPreviewCartItems(memberId, cartIds))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);
    }

    @Test
    void getOrderPreviewCartItemsThrowsWhenOnlyNullCartIdsAreProvided() {
        Long memberId = 1L;
        List<Long> cartIds = Arrays.asList(null, null);

        assertThatThrownBy(() -> cartService.getOrderPreviewCartItems(memberId, cartIds))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);
    }

    private Product createProduct(String name, int price, int stock, ProductStatus status) {
        return Product.create(name, price, stock, status, "description", null);
    }

    private Member createMember() {
        return Member.create("member@example.com", "password", "member", "010-1234-5678");
    }
}
