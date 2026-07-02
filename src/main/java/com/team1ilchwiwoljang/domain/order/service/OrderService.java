package com.team1ilchwiwoljang.domain.order.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.cart.entity.Cart;
import com.team1ilchwiwoljang.domain.cart.service.CartService;
import com.team1ilchwiwoljang.domain.category.entity.Category;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import com.team1ilchwiwoljang.domain.order.dto.request.CartOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderPreviewRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.request.AdminOrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.dto.response.AdminOrderDetailResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.AdminOrderSearchResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.DirectOrderPreviewResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.DirectOrderPreviewResponse.DirectOrderPreviewItemResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderItemResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderPreviewResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderPreviewResponse.OrderPreviewItemResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import com.team1ilchwiwoljang.domain.order.repository.OrderItemRepository;
import com.team1ilchwiwoljang.domain.order.repository.OrderRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.order.dto.request.OrderSearchCondition;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderHistoryResponse;
import com.team1ilchwiwoljang.domain.product.service.ProductService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Transactional(readOnly = true)
    public PageResponse<OrderHistoryResponse> getOrderHistory(Long memberId, OrderSearchCondition condition, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findOrderHistoryByMemberId(memberId, condition, pageable);

        // 주문 목록은 상세 품목을 펼치지 않고, 상세보기 API에서 사용할 주문 식별 정보만 내려준다.
        Page<OrderHistoryResponse> dtoPage = orderPage.map(order -> OrderHistoryResponse.of(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getOrderStatus().name(),
                order.getCreatedAt()
        ));

        return PageResponse.from(dtoPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminOrderSearchResponse> getAdminOrders(AdminOrderSearchCondition condition, Pageable pageable) {
        validateAdminOrderSearchCondition(condition);
        Page<Order> orderPage = orderRepository.findAdminOrders(condition, pageable);
        return PageResponse.from(orderPage.map(AdminOrderSearchResponse::from));
    }

    private void validateAdminOrderSearchCondition(AdminOrderSearchCondition condition) {
        if (condition.startDate() != null && condition.endDate() != null
                && condition.startDate().isAfter(condition.endDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (condition.minTotalAmount() != null && condition.minTotalAmount() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (condition.maxTotalAmount() != null && condition.maxTotalAmount() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
        if (condition.minTotalAmount() != null && condition.maxTotalAmount() != null
                && condition.minTotalAmount() > condition.maxTotalAmount()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getAdminOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderItemResponse> orderItems = orderItemRepository.findByOrderIdIn(List.of(orderId)).stream()
                .map(OrderItemResponse::from)
                .toList();

        return AdminOrderDetailResponse.from(order, orderItems);
    }

    @Transactional(readOnly = true)
    public DirectOrderPreviewResponse previewDirectOrder(DirectOrderPreviewRequest request) {
        Product product = productService.getProduct(request.productId());

        validateOrderableProduct(product, request.quantity());

        DirectOrderPreviewItemResponse orderItem = createDirectOrderPreviewItemResponse(product, request.quantity());
        List<DirectOrderPreviewItemResponse> orderItems = List.of(orderItem);
        long totalOrderAmount = calculateDirectTotalOrderAmount(orderItems);

        return DirectOrderPreviewResponse.of(orderItems, totalOrderAmount);
    }


    @Transactional
    public OrderResponse createDirectOrder(Long memberId, DirectOrderRequest request) {
        Member member = memberService.getMember(memberId);
        Product product = productService.getProduct(request.productId());

        validateOrderableProduct(product, request.quantity());

        Long totalAmount = (long) product.getPrice() * request.quantity();
        String orderNumber = UUID.randomUUID().toString();

        Order order = Order.create(member, orderNumber, totalAmount, totalAmount);
        orderRepository.save(order);

        Category category = product.getCategory();
        OrderItem orderItem = OrderItem.create(
                order,
                product,
                product.getName(),
                (long) product.getPrice(),
                (long) request.quantity(),
                totalAmount,
                category != null ? category.getId() : null,
                category != null ? category.getName() : null
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

    private void validateCartOrderItems(List<Cart> cartItems) {
        for (Cart cart : cartItems) {
            validateOrderableProduct(cart.getProduct(), cart.getQuantity());
        }
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
     * 장바구니 기반 미리보기는 cartId가 필요하므로 바로 주문 미리보기 응답과 분리합니다.
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
     * 바로 주문 미리보기 응답 항목을 생성합니다.
     * 바로 주문은 아직 장바구니에 담긴 상품이 아니므로 cartId를 응답하지 않습니다.
     */
    private DirectOrderPreviewItemResponse createDirectOrderPreviewItemResponse(Product product, int quantity) {
        long productPrice = product.getPrice();
        long productTotalAmount = productPrice * quantity;

        return DirectOrderPreviewItemResponse.of(
                product.getId(),
                product.getName(),
                productPrice,
                quantity,
                productTotalAmount
        );
    }

    /**
     * 바로 주문 미리보기의 총 상품 금액을 계산합니다.
     */
    private long calculateDirectTotalOrderAmount(List<DirectOrderPreviewItemResponse> orderItems) {
        return orderItems.stream()
                .mapToLong(DirectOrderPreviewItemResponse::productTotalAmount)
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

        Category category = product.getCategory();
        return OrderItem.create(
                order,
                product,
                product.getName(),
                productPrice,
                quantity,
                totalPrice,
                category != null ? category.getId() : null,
                category != null ? category.getName() : null
        );
    }
}
