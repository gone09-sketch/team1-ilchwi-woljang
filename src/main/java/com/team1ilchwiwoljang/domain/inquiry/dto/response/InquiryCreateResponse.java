package com.team1ilchwiwoljang.domain.inquiry.dto.response;

import com.team1ilchwiwoljang.domain.inquiry.entity.Inquiry;
import com.team1ilchwiwoljang.domain.inquiry.entity.InquiryStatus;
import java.time.LocalDateTime;

public record InquiryCreateResponse(
        Long id,
        Long memberId,
        String title,
        String content,
        InquiryStatus status,
        LocalDateTime createdAt
) {
    public static InquiryCreateResponse from(Inquiry inquiry) {
        return new InquiryCreateResponse(
                inquiry.getId(),
                inquiry.getMember().getId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.getStatus(),
                inquiry.getCreatedAt()
        );
    }
}
