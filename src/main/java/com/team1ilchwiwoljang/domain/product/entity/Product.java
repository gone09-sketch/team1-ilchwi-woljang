package com.team1ilchwiwoljang.domain.product.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.category.entity.Category;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_product_status_created_at", columnList = "status, created_at"),
                @Index(name = "idx_product_category_status_created_at", columnList = "category_id, status, created_at"),
                @Index(name = "idx_product_status_price", columnList = "status, price")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private int salesCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    @Column(nullable = false)
    private String description;

    private Product(String name, int price, int stock, ProductStatus status, String description, Category category) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.salesCount = 0; // 초기값 0 세팅
        this.status = status;
        this.description = description;
        this.category = category;
    }

    public static Product create(String name, int price, int stock, ProductStatus status, String description, Category category) {
        return new Product(name, price, stock, status, description, category);
    }

    public boolean isOnSale() {
        return status == ProductStatus.ON_SALE;
    }

    public void decreaseStock(int quantity) {
        if (this.stock < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
        this.stock -= quantity;
    }

    public void increaseSalesCount(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("판매량 증가는 음수일 수 없습니다.");
        }
        this.salesCount += quantity;
    }
}
