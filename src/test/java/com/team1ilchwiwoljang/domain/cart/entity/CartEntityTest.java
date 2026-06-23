package com.team1ilchwiwoljang.domain.cart.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import org.junit.jupiter.api.Test;

class CartEntityTest {

    @Test
    void cartExtendsBaseEntity() {
        assertThat(BaseEntity.class.isAssignableFrom(Cart.class)).isTrue();
    }

    @Test
    void createRejectsZeroOrNegativeQuantity() {
        Member member = Member.create("test@example.com", "password", "테스터", "010-1234-5678");
        Product product = Product.create("상품", 1_000, 10, ProductStatus.ON_SALE);

        assertThatThrownBy(() -> Cart.create(member, product, 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_QUANTITY));
        assertThatThrownBy(() -> Cart.create(member, product, -1))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_QUANTITY));
    }

    @Test
    void increaseQuantityRejectsWhenResultQuantityIsZeroOrNegative() {
        Cart cart = Cart.create(
                Member.create("test@example.com", "password", "테스터", "010-1234-5678"),
                Product.create("상품", 1_000, 10, ProductStatus.ON_SALE),
                2);

        assertThatThrownBy(() -> cart.increaseQuantity(-2))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_QUANTITY));
    }
}
