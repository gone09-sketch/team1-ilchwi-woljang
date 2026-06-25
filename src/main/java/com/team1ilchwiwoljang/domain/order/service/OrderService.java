package com.team1ilchwiwoljang.domain.order.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import com.team1ilchwiwoljang.domain.order.dto.request.DirectOrderRequest;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderItemResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderResponse;
import com.team1ilchwiwoljang.domain.order.entity.Order;
import com.team1ilchwiwoljang.domain.order.entity.OrderItem;
import com.team1ilchwiwoljang.domain.order.repository.OrderItemRepository;
import com.team1ilchwiwoljang.domain.order.repository.OrderRepository;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import com.team1ilchwiwoljang.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderHistoryResponse;
import com.team1ilchwiwoljang.domain.order.dto.response.OrderItemHistoryResponse;
import com.team1ilchwiwoljang.domain.order.dto.request.OrderSearchCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<OrderHistoryResponse> getOrderHistory(Long memberId, OrderSearchCondition condition, Pageable pageable) {
        Page<Order> orderPage = orderRepository.findOrderHistoryByMemberId(memberId, condition, pageable);


        List<Long> orderIds = orderPage.getContent().stream()
                .map(Order::getId)
                .toList();

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdIn(orderIds);

        Map<Long, List<OrderItemHistoryResponse>> itemsByOrderId = orderItems.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getOrder().getId(),
                        Collectors.mapping(
                                item -> OrderItemHistoryResponse.of(
                                        item.getProduct().getId(),
                                        item.getProductNameSnapshot(),
                                        item.getProductPriceSnapshot(),
                                        item.getQuantity(),
                                        item.getTotalPrice()
                                ),
                                Collectors.toList()
                        )
                ));

        Page<OrderHistoryResponse> dtoPage = orderPage.map(order -> OrderHistoryResponse.of(
                order.getId(),
                order.getOrderNumber(),
                order.getTotalAmount(),
                order.getOrderStatus().name(),
                order.getCreatedAt(),
                itemsByOrderId.getOrDefault(order.getId(), List.of())
        ));

        return PageResponse.from(dtoPage);
    }

    @Transactional
    public OrderResponse createDirectOrder(Long memberId, DirectOrderRequest request) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getStock() < request.quantity()) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }

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


}
