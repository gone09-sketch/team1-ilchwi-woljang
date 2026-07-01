package com.team1ilchwiwoljang.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.repository.CartRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import com.team1ilchwiwoljang.domain.order.dto.request.CartOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import com.team1ilchwiwoljang.domain.order.repository.OrderItemRepository;
import com.team1ilchwiwoljang.domain.order.repository.OrderRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @AfterEach
    void tearDown() {
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        cartRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("장바구니 주문 생성 시 주문, 주문상품 저장과 재고 감소, 장바구니 삭제가 함께 반영된다")
    void given_cartItems_whenCreateCartOrder_thenPersistOrderAndDeleteCartsAndDecreaseStock() {
        Member member = memberRepository.save(
                Member.create("member@example.com", "password", "member", "010-1234-5678")
        );
        Product keyboard = productRepository.save(createProduct("keyboard", 10_000, 10));
        Product mouse = productRepository.save(createProduct("mouse", 5_000, 8));
        Cart keyboardCart = cartRepository.save(Cart.create(member, keyboard, 2));
        Cart mouseCart = cartRepository.save(Cart.create(member, mouse, 3));
        CartOrderRequest request = new CartOrderRequest(List.of(keyboardCart.getId(), mouseCart.getId()));

        OrderResponse response = orderService.createCartOrder(member.getId(), request);

        List<Order> orders = orderRepository.findAll();
        List<OrderItem> orderItems = orderItemRepository.findAll();
        Product savedKeyboard = productRepository.findById(keyboard.getId()).orElseThrow();
        Product savedMouse = productRepository.findById(mouse.getId()).orElseThrow();

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getId()).isEqualTo(response.orderId());
        assertThat(orders.get(0).getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orders.get(0).getTotalAmount()).isEqualTo(35_000L);

        assertThat(orderItems).hasSize(2);
        assertThat(orderItems)
                .extracting(OrderItem::getProductNameSnapshot)
                .containsExactlyInAnyOrder("keyboard", "mouse");
        assertThat(orderItems)
                .extracting(OrderItem::getTotalPrice)
                .containsExactlyInAnyOrder(20_000L, 15_000L);

        assertThat(savedKeyboard.getStock()).isEqualTo(8);
        assertThat(savedKeyboard.getSalesCount()).isEqualTo(2);
        assertThat(savedMouse.getStock()).isEqualTo(5);
        assertThat(savedMouse.getSalesCount()).isEqualTo(3);
        assertThat(cartRepository.findAll()).isEmpty();
    }

    private Product createProduct(String name, int price, int stock) {
        return Product.create(name, price, stock, ProductStatus.ON_SALE, "description", null);
    }
}
