package com.team1ilchwiwoljang.domain.order.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.service.CartService;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderPreviewRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderItemResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderPreviewResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import com.team1ilchwiwoljang.domain.order.repository.OrderItemRepository;
import com.team1ilchwiwoljang.domain.order.repository.OrderRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final Clock clock;
    private final MemberService memberService;
    private final ProductService productService;
    private final CartService cartService;

    /**
     * 상품 상세 페이지에서 바로 주문하기 전 주문서 미리보기 정보를 생성합니다.
     * 실제 주문 저장, 주문상품 저장, 재고 차감은 하지 않습니다.
     */
    @Transactional(readOnly = true)
    public OrderPreviewResponse previewDirectOrder(DirectOrderPreviewRequest request) {
        Product product = productService.getProduct(request.productId());

        validateOrderableProduct(product, request.quantity());

        OrderItemResponse orderItem = createOrderPreviewItemResponse(product, request.quantity());
        List<OrderItemResponse> orderItems = List.of(orderItem);
        long totalAmount = calculateTotalOrderAmount(orderItems);

        return OrderPreviewResponse.of(orderItems, totalAmount);
    }

    /**
     * 상품 상세 페이지에서 바로 주문을 생성하는 메서드입니다.
     * 회원과 상품을 조회한 뒤, 상품 상태와 재고를 검증하고 주문/주문상품을 저장합니다.
     */
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
    public void cancelOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.cancel(LocalDateTime.now(clock));
    }

    /**
     * 장바구니 상품을 주문서 형태로 미리 확인하는 메서드입니다.
     * 실제 주문을 저장하거나 재고를 차감하지 않고, 주문 가능한 상태인지 검증한 뒤 미리보기 응답만 반환합니다.
     */
    @Transactional(readOnly = true)
    public OrderPreviewResponse previewOrder(Long memberId, List<Long> cartIds) {
        List<Cart> cartItems = cartService.getOrderPreviewCartItems(memberId, cartIds);
        validatePreviewCartItems(cartItems);

        List<OrderPreviewItemResponse> orderItems = cartItems.stream()
                .map(this::toOrderPreviewItemResponse)
                .toList();

        long totalOrderAmount = calculateTotalOrderAmount(orderItems);

        return OrderPreviewResponse.of(orderItems, totalOrderAmount);
    }

    /**
     * 장바구니 엔티티에서 주문서 미리보기에 필요한 상품 정보와 수량만 꺼내 응답 항목으로 변환합니다.
     * 응답 DTO가 Cart 엔티티 구조를 직접 알지 않도록 변환 책임을 서비스에 둡니다.
     */
    private OrderPreviewItemResponse toOrderPreviewItemResponse(Cart cart) {
        Product product = cart.getProduct();

        return createOrderPreviewItemResponse(cart.getId(), product, cart.getQuantity());
    }

    /**
     * 상품과 수량을 기준으로 주문서 미리보기 응답 항목을 생성합니다.
     * 나중에 바로 구매 미리보기가 추가되어도 이 메서드를 재사용할 수 있습니다.
     */
    private OrderPreviewItemResponse createOrderPreviewItemResponse(Long cartId, Product product, int quantity) {
        long productPrice = product.getPrice();
        long productTotalAmount = productPrice * quantity;

        return OrderPreviewItemResponse.of(
                cartId,
                product.getId(),
                product.getName(),
                productPrice,
                quantity,
                productTotalAmount
        );
    }

    /**
     * 주문서 미리보기의 총 상품 금액을 계산합니다.
     */
    private long calculateTotalOrderAmount(List<OrderPreviewItemResponse> orderItems) {
        return orderItems.stream()
                .mapToLong(OrderPreviewItemResponse::productTotalAmount)
                .sum();
    }

    /**
     * 주문서 미리보기 대상 장바구니 상품들이 실제 주문 가능한지 검증하는 메서드입니다.
     * 장바구니가 비어 있으면 주문서가 될 수 없고, 각 상품은 ON_SALE 상태이며 재고가 충분해야 합니다.
     */
    private void validatePreviewCartItems(List<Cart> cartItems) {
        if (cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_ORDER_PREVIEW);
        }

        for (Cart cart : cartItems) {
            validateOrderableProduct(cart.getProduct(), cart.getQuantity());
        }
    }

    /**
     * 상품 하나가 주문 가능한 상태인지 검증하는 공통 메서드입니다.
     * 주문 생성과 주문서 미리보기 모두 같은 기준으로 검증하기 위해 분리했습니다.
     */
    private void validateOrderableProduct(Product product, int quantity) {
        if (!product.isOnSale()) {
            throw new BusinessException(ErrorCode.NOT_ORDERABLE_PRODUCT);
        }

        if (product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
    }
}


    /**
     * 상품과 수량을 기준으로 주문서 미리보기 응답 항목을 생성합니다.
     */
    private OrderItemResponse createOrderPreviewItemResponse(Product product, int quantity) {
        long productPrice = product.getPrice();
        long productTotalAmount = productPrice * quantity;

        return OrderItemResponse.of(
                product.getName(),
                productPrice,
                quantity,
                productTotalAmount
        );
    }

    /**
     * 주문서 미리보기의 총 상품 금액을 계산합니다.
     */
    private long calculateTotalOrderAmount(List<OrderItemResponse> orderItems) {
        return orderItems.stream()
                .mapToLong(OrderItemResponse::productTotalAmount)
                .sum();
    }

    /**
     * 상품이 주문 가능한 상태인지 검증합니다.
     */
    private void validateOrderableProduct(Product product, int quantity) {
        if (!product.isOnSale()) {
            throw new BusinessException(ErrorCode.NOT_ORDERABLE_PRODUCT);
        }

        if (product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
    }
}
