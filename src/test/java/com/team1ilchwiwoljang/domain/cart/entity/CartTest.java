package com.team1ilchwiwoljang.domain.cart.entity;

import static org.assertj.core.api.Assertions.assertThat;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class CartTest {

    @Test
    void createInitializesMemberProductAndQuantity() {
        Member member = Member.create("member@example.com", "password", "회원", "010-1234-5678");
        Product product = Product.create("상품", 1_000, 10, ProductStatus.ON_SALE);

        Cart cart = Cart.create(member, product, 2);

        assertThat(cart.getMember()).isSameAs(member);
        assertThat(cart.getProduct()).isSameAs(product);
        assertThat(cart.getQuantity()).isEqualTo(2);
    }

    @Test
    void increaseQuantityAddsPositiveQuantity() {
        Cart cart = Cart.create(
                Member.create("member@example.com", "password", "회원", "010-1234-5678"),
                Product.create("상품", 1_000, 10, ProductStatus.ON_SALE),
                2
        );

        cart.increaseQuantity(3);

        assertThat(cart.getQuantity()).isEqualTo(5);
    }

    @Test
    void memberAndProductAreUniqueCartKey() {
        Table table = Cart.class.getAnnotation(Table.class);

        assertThat(table).isNotNull();
        assertThat(table.uniqueConstraints())
                .anySatisfy(uniqueConstraint -> assertThat(uniqueConstraint.columnNames())
                        .containsExactly("member_id", "product_id"));
    }

    @Test
    void productJoinColumnAllowsNullForErdCompatibility() throws NoSuchFieldException {
        Field productField = Cart.class.getDeclaredField("product");
        JoinColumn joinColumn = productField.getAnnotation(JoinColumn.class);

        assertThat(joinColumn).isNotNull();
        assertThat(joinColumn.name()).isEqualTo("product_id");
        assertThat(joinColumn.nullable()).isTrue();
    }
}
