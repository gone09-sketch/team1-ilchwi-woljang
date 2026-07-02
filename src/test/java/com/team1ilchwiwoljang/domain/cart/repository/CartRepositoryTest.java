package com.team1ilchwiwoljang.domain.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.team1ilchwiwoljang.common.config.QueryDslConfig;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import jakarta.persistence.PersistenceUnitUtil;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("선택한 장바구니 ID 중 요청 회원의 장바구니만 상품과 함께 조회한다")
    void findAllByMemberIdAndIdInWithProductReturnsOnlyMemberCartsWithProduct() {
        Member member = persistMember("member@example.com");
        Member otherMember = persistMember("other@example.com");
        Product keyboard = persistProduct("keyboard");
        Product mouse = persistProduct("mouse");
        Cart memberCart = persistCart(member, keyboard, 1);
        Cart otherMemberCart = persistCart(otherMember, mouse, 1);

        entityManager.flush();
        entityManager.clear();

        List<Cart> cartItems = cartRepository.findAllByMemberIdAndIdInWithProduct(
                member.getId(),
                List.of(memberCart.getId(), otherMemberCart.getId())
        );

        assertThat(cartItems)
                .extracting(Cart::getId)
                .containsExactly(memberCart.getId());

        PersistenceUnitUtil persistenceUnitUtil = entityManager.getEntityManager()
                .getEntityManagerFactory()
                .getPersistenceUnitUtil();

        assertThat(persistenceUnitUtil.isLoaded(cartItems.get(0).getProduct())).isTrue();
    }

    private Member persistMember(String email) {
        Member member = Member.create(email, "password", "member", "010-1234-5678");

        return entityManager.persist(member);
    }

    private Product persistProduct(String name) {
        Product product = Product.create(name, 10_000, 10, ProductStatus.ON_SALE, "description", null);

        return entityManager.persist(product);
    }

    private Cart persistCart(Member member, Product product, int quantity) {
        Cart cart = Cart.create(member, product, quantity);

        return entityManager.persist(cart);
    }
}
