package com.team1ilchwiwoljang.domain.product.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    private Product(String name, int price, int stock, ProductStatus status) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.status = status;
    }

    public static Product create(String name, int price, int stock, ProductStatus status) {
        return new Product(name, price, stock, status);
    }

    public boolean isOnSale() {
        return status == ProductStatus.ON_SALE;
    }

    public void decreaseStock(int quantity) {
        this.stock -= quantity;
    }
}
