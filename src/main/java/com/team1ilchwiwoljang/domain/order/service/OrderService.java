package com.team1ilchwiwoljang.domain.order.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.service.CartService;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.order.dto.request.CartOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderItemResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import com.team1ilchwiwoljang.domain.order.repository.OrderItemRepository;
import com.team1ilchwiwoljang.domain.order.repository.OrderRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductService productService;
    private final MemberService memberService;
    private final CartService cartService;

    @Transactional
    public OrderResponse createDirectOrder(Long memberId, DirectOrderRequest request) {

        Member member = memberService.getMember(memberId);
        Product product = productService.getProduct(request.productId());

        validateOrderableProduct(product, request.quantity());

        Long totalAmount = (long) product.getPrice() * request.quantity();
        String orderNumber = UUID.randomUUID().toString();

        Order order = Order.create(member, orderNumber, totalAmount, totalAmount);
        orderRepository.save(order);

        OrderItem orderItem = OrderItem.create(
                order,
                product,
                product.getName(),
                (long) product.getPrice(),
                (long) request.quantity(),
                totalAmount
        );
        orderItemRepository.save(orderItem);

        product.decreaseStock(request.quantity());

        List<OrderItemResponse> orderItems = List.of(OrderItemResponse.from(orderItem));
        return OrderResponse.from(order, orderItems);
    }

    @Transactional
    public OrderResponse createCartOrder(Long memberId, CartOrderRequest request) {

        Member member = memberService.getMember(memberId);

        List<Cart> cartItems = cartService.getOrderCartItems(memberId, request.cartIds());
        validateCartOrderItems(cartItems);

        Long totalAmount = calculateCartOrderTotalAmount(cartItems);
        String orderNumber = UUID.randomUUID().toString();

        Order order = Order.create(member, orderNumber, totalAmount, totalAmount);
        orderRepository.save(order);

        List<OrderItem> orderItems = cartItems.stream()
                .map(cart -> createOrderItem(order, cart))
                .toList();
        orderItemRepository.saveAll(orderItems);

        // 주문 저장, 재고 차감, 장바구니 삭제는 같은 트랜잭션 안에서 함께 성공하거나 함께 실패해야 합니다.
        cartItems.forEach(cart -> cart.getProduct().decreaseStock(cart.getQuantity()));
        cartService.deleteOrderCartItems(cartItems);

        List<OrderItemResponse> orderItemResponses = orderItems.stream()
                .map(OrderItemResponse::from)
                .toList();
        return OrderResponse.from(order, orderItemResponses);
    }

    private void validateCartOrderItems(List<Cart> cartItems) {
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_CART_ORDER);
        }

        for (Cart cart : cartItems) {
            validateOrderableProduct(cart.getProduct(), cart.getQuantity());
        }
    }

    private void validateOrderableProduct(Product product, int quantity) {
        if (!product.isOnSale()) {
            throw new BusinessException(ErrorCode.NOT_ORDERABLE_PRODUCT);
        }

        if (product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
    }

    private Long calculateCartOrderTotalAmount(List<Cart> cartItems) {
        return cartItems.stream()
                .mapToLong(cart -> (long) cart.getProduct().getPrice() * cart.getQuantity())
                .sum();
    }

    private OrderItem createOrderItem(Order order, Cart cart) {
        Product product = cart.getProduct();
        long productPrice = (long) product.getPrice();
        long quantity = (long) cart.getQuantity();
        long totalPrice = productPrice * quantity;

        return OrderItem.create(
                order,
                product,
                product.getName(),
                productPrice,
                quantity,
                totalPrice
        );
    }
}
