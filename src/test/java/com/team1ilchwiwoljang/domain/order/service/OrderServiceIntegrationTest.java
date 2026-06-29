package com.team1ilchwiwoljang.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.repository.CartRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import com.team1ilchwiwoljang.domain.order.dto.request.CartOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import com.team1ilchwiwoljang.domain.order.repository.OrderItemRepository;
import com.team1ilchwiwoljang.domain.order.repository.OrderRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.entity.ProductStatus;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
        assertThat(savedMouse.getStock()).isEqualTo(5);
        assertThat(cartRepository.findAll()).isEmpty();
    }

    private Product createProduct(String name, int price, int stock) {
        return Product.create(name, price, stock, ProductStatus.ON_SALE, "description", null);
    }
    @Test
    void 동시에_직접_주문을_요청해도_재고보다_많은_주문은_생성되지_않는다() throws InterruptedException {
        int stock = 10;
        int requestCount = 20;

        // given
        Member member = memberRepository.save(
                Member.create("test@example.com", "password", "tester", "010-1111-2222")
        );

        Product product = productRepository.save(
                Product.create("테스트 상품", 10000, stock, ProductStatus.ON_SALE, "설명", null)
        );

        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try{
                    readyLatch.countDown();
                    startLatch.await();

                    orderService.createDirectOrder(
                            member.getId(),
                            new DirectOrderRequest(product.getId(), 1)
                    );

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executorService.shutdown();

        // then
        Product savedProduct = productRepository.findById(product.getId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(stock);
        assertThat(failCount.get()).isEqualTo(requestCount - stock);
        assertThat(savedProduct.getStock()).isEqualTo(0);

    }
    @Test
    void 동시에_장바구니_주문을_요청해도_재고보다_많은_주문은_생성되지_않는다() throws InterruptedException {
        int stock = 10;
        int requestCount = 20;

        //given
        Product product = productRepository.save(
                Product.create("장바구니 테스트 상품", 10000, stock, ProductStatus.ON_SALE, "설명", null)
        );

        List<Member> members = new ArrayList<>();
        List<Cart> carts = new ArrayList<>();

        //when
        for (int i = 0; i < requestCount; i++){
            Member member = memberRepository.save(
                    Member.create("cart-user" + i + "@test.com", "password", "user" + i, "010-0000-" + String.format("%04d", i))
            );

            Cart cart = cartRepository.save(
                    Cart.create(member, product, 1)
            );

            members.add(member);
            carts.add(cart);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger outOfStockFailCount = new AtomicInteger();
        AtomicInteger unexpectedFailCount = new AtomicInteger();

        for (int i = 0; i < requestCount; i++){
            int index = i;

            executorService.submit(() -> {
                try{
                    readyLatch.countDown();
                    startLatch.await();

                    orderService.createCartOrder(
                            members.get(index).getId(),
                            new CartOrderRequest(List.of(carts.get(index).getId()))
                    );

                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if (e.getErrorCode() == ErrorCode.OUT_OF_STOCK){
                        outOfStockFailCount.incrementAndGet();
                    } else {
                        unexpectedFailCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    unexpectedFailCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executorService.shutdown();

        Product savedProduct = productRepository.findById(product.getId()).orElseThrow();

        assertThat(successCount.get()).isEqualTo(stock);
        assertThat(outOfStockFailCount.get()).isEqualTo(requestCount - stock);
        assertThat(savedProduct.getStock()).isEqualTo(0);
    }

}
