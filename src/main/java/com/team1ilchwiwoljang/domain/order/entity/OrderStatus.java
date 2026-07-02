package com.team1ilchwiwoljang.domain.order.entity;

public enum OrderStatus {
    PENDING,
    PAID,
    CANCELLED;

    public boolean canChangeTo(OrderStatus newStatus){
        if (this == CANCELLED){
            return false;
        }

        return true;
    }
}
