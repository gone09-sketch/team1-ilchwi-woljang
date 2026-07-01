package com.team1ilchwiwoljang.domain.order.dto.request;

import com.team1ilchwiwoljang.domain.order.entity.OrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AdminOrderSearchCondition(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate endDate,
        OrderStatus orderStatus,
        Long minTotalAmount,
        Long maxTotalAmount,
        String keyword,
        Long memberId
) {
}
