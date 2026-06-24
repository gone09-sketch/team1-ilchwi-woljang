package com.team1ilchwiwoljang.domain.order.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 40)
    private String orderNumber;

    @Column(nullable = false)
    private Long totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    private Long pgAmount;

    private LocalDateTime paidAt;

    private LocalDateTime canceledAt;

    public static Order create(Member member, String orderNumber, Long totalAmount, Long pgAmount) {
        Order order = new Order();
        order.member = member;
        order.orderNumber = orderNumber;
        order.totalAmount = totalAmount;
        order.pgAmount = pgAmount;
        order.orderStatus = OrderStatus.PENDING;
        return order;
    }

    // 상태 변경 메서드 추가
    public void updateStatus(OrderStatus orderStatus){
        this.orderStatus = orderStatus;

        if(orderStatus == OrderStatus.PAID){
            this.paidAt = LocalDateTime.now();
        }

        if (orderStatus == OrderStatus.CANCELLED){
            this.canceledAt = LocalDateTime.now();
        }
    }
}
