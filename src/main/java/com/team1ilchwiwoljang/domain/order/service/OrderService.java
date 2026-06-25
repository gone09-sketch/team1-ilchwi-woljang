package com.team1ilchwiwoljang.domain.order.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
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
