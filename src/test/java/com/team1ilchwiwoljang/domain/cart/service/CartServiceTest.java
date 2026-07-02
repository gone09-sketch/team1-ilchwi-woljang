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
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

    @Mock
    private MemberService memberService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("장바구니 목록을 조회하면 상품 목록과 총 금액을 반환한다")
    void getCartReturnsItemsAndTotalPrice() {
        Long memberId = 1L;
        Product product = createProduct("keyboard", 1_000, 10, ProductStatus.ON_SALE);
        Cart cart = Cart.create(createMember(), product, 3);
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
        Product product = createProduct("keyboard", 1_000, 2, ProductStatus.ON_SALE);
        Cart cart = Cart.create(createMember(), product, 3);
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
    @DisplayName("장바구니 상품 수량을 변경하면 변경된 상품 응답을 반환한다")
    void updateCartItemQuantityReturnsUpdatedItem() {
        Long memberId = 1L;
        Long cartItemId = 10L;
        Product product = createProduct("keyboard", 1_000, 10, ProductStatus.ON_SALE);
        Cart cart = Cart.create(createMember(), product, 1);
        given(cartRepository.findByIdAndMemberIdWithProduct(cartItemId, memberId))
                .willReturn(Optional.of(cart));

        CartItemResponse response = cartService.updateCartItemQuantity(memberId, cartItemId, 4);

        assertThat(response.quantity()).isEqualTo(4);
        assertThat(response.itemTotalPrice()).isEqualTo(4_000L);
        assertThat(response.orderable()).isTrue();
    }

    @Test
    @DisplayName("변경할 장바구니 수량이 재고보다 많으면 예외가 발생한다")
    void updateCartItemQuantityThrowsWhenQuantityExceedsStock() {
        Long memberId = 1L;
        Long cartItemId = 10L;
        Product product = createProduct("keyboard", 1_000, 2, ProductStatus.ON_SALE);
        Cart cart = Cart.create(createMember(), product, 1);
        given(cartRepository.findByIdAndMemberIdWithProduct(cartItemId, memberId))
                .willReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateCartItemQuantity(memberId, cartItemId, 3))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_QUANTITY_EXCEEDED);
    }

    @Test
    @DisplayName("회원의 장바구니 상품을 삭제한다")
    void deleteCartItemDeletesMemberCartItem() {
        Long memberId = 1L;
        Long cartItemId = 10L;
        Cart cart = Cart.create(createMember(), createProduct("keyboard", 1_000, 10, ProductStatus.ON_SALE), 1);
        given(cartRepository.findByIdAndMemberIdWithProduct(cartItemId, memberId))
                .willReturn(Optional.of(cart));

        cartService.deleteCartItem(memberId, cartItemId);

        verify(cartRepository).delete(cart);
    }

    @Test
    @DisplayName("주문 대상 장바구니 ID를 중복 제거한 뒤 선택된 장바구니 상품을 조회한다")
    void getOrderCartItemsReturnsSelectedCartItems() {
        Long memberId = 1L;
        List<Long> cartIds = List.of(10L, 10L, 20L);
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
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMPTY_CART_ORDER);
    }

    @Test
    @DisplayName("주문 대상 장바구니 ID에 null이 있으면 INVALID_CART_ITEM_ID 예외가 발생한다")
    void getOrderCartItemsThrowsWhenCartIdsContainNull() {
        Long memberId = 1L;
        List<Long> cartIds = Arrays.asList(10L, null);

        assertThatThrownBy(() -> cartService.getOrderCartItems(memberId, cartIds))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CART_ITEM_ID);
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
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CART_ITEM_NOT_FOUND);
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
    @DisplayName("cartIds에 null이 하나라도 있으면 잘못된 요청으로 처리한다")
    void getOrderPreviewCartItemsThrowsWhenCartIdsContainNull() {
        Long memberId = 1L;
        List<Long> cartIds = Arrays.asList(null, 10L);

        assertThatThrownBy(() -> cartService.getOrderPreviewCartItems(memberId, cartIds))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CART_ITEM_ID);
    }

    private Member createMember() {
        return Member.create("member@example.com", "password", "member", "010-1234-5678");
    }

    private Product createProduct(String name, int price, int stock, ProductStatus status) {
        return Product.create(name, price, stock, status, "description", null);
    }
}
