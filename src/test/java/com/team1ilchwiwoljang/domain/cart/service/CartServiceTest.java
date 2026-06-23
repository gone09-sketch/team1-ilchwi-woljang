package com.team1ilchwiwoljang.domain.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.team1ilchwiwoljang.domain.cart.dto.response.CartItemResponse;
import com.team1ilchwiwoljang.domain.cart.dto.response.CartResponse;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.repository.CartRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import java.util.List;
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
        Member member = createMember();
        Product product = Product.create("상품", 1_000, 10, ProductStatus.ON_SALE);
        Cart cart = Cart.create(member, product, 3);
        given(cartRepository.findAllByMemberIdWithProduct(memberId))
                .willReturn(List.of(cart));

        CartResponse response = cartService.getCart(memberId);

        assertThat(response.cartTotalPrice()).isEqualTo(3_000L);
        assertThat(response.items()).hasSize(1);

        CartItemResponse item = response.items().get(0);
        assertThat(item.productName()).isEqualTo("상품");
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
        Member member = createMember();
        Product product = Product.create("상품", 1_000, 2, ProductStatus.ON_SALE);
        Cart cart = Cart.create(member, product, 3);
        given(cartRepository.findAllByMemberIdWithProduct(memberId))
                .willReturn(List.of(cart));

        CartResponse response = cartService.getCart(memberId);

        assertThat(response.items().get(0).orderable()).isFalse();
    }

    @Test
    void getCartReturnsNotOrderableWhenProductIsNotOnSale() {
        Long memberId = 1L;
        Member member = createMember();
        Product outOfStockProduct = Product.create("품절 상품", 1_000, 10, ProductStatus.OUT_OF_STOCK);
        Product stoppedProduct = Product.create("판매중지 상품", 2_000, 10, ProductStatus.STOPPED);
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

    private Member createMember() {
        return Member.create("member@example.com", "password", "회원", "010-1234-5678");
    }
}
