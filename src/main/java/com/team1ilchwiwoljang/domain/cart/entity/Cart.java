package com.team1ilchwiwoljang.domain.cart.entity;

import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.product.entity.Product;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "carts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_carts_member_product",
                        columnNames = {"member_id", "product_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = true)
    private Product product;

    @Column(nullable = false, columnDefinition = "int UNSIGNED DEFAULT 1")
    private int quantity;

    private Cart(Member member, Product product, int quantity) {
        this.member = member;
        this.product = product;
        this.quantity = quantity;
    }

    public static Cart create(Member member, Product product, int quantity) {
        return new Cart(member, product, quantity);
    }

    public void increaseQuantity(int quantity) {
        this.quantity += quantity;
    }
}
