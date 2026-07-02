package com.team1ilchwiwoljang.domain.inquiry.dto.response;

import com.team1ilchwiwoljang.domain.inquiry.entity.Inquiry;
import com.team1ilchwiwoljang.domain.inquiry.entity.InquiryStatus;

import java.time.LocalDateTime;

public record InquiryAdminResponse(
        Long id,
        Long memberId,
        String title,
        String content,
        Long adminId,
        String answer,
        InquiryStatus status,
        LocalDateTime createdAt,
        LocalDateTime answeredAt
) {
    public static InquiryAdminResponse from(Inquiry inquiry) {
        return new InquiryAdminResponse(
                inquiry.getId(),
                inquiry.getMember().getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getAdminId(),
                inquiry.getAnswer(),
                inquiry.getStatus(),
                inquiry.getCreatedAt(),
                inquiry.getAnsweredAt()
        );
    }
}
