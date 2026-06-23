package com.team1ilchwiwoljang.domain.order.entity;


import com.team1ilchwiwoljang.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;

    @Column(nullable = false, length = 100)
    private String productNameSnapshot;

    @Column(nullable = false)
    private Long productPriceSnapshot;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private Long totalPrice;

    public static OrderItem create(Order order, Product product,
            String productNameSnapshot, Long productPriceSnapshot,
            Long quantity, Long totalPrice) {
        OrderItem item = new OrderItem();
        item.order = order;
        item.product = product;
        item.productNameSnapshot = productNameSnapshot;
        item.productPriceSnapshot = productPriceSnapshot;
        item.quantity = quantity;
        item.totalPrice = totalPrice;
        return item;
    }
}
