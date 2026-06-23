package com.team1ilchwiwoljang.domain.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InquiryCreateRequest(
        @NotBlank(message = "제목은 필수 입력 값입니다.")
        @Size(max = 100, message = "제목은 최대 100자까지 입력 가능합니다.")
        String title,

        @NotBlank(message = "내용은 필수 입력 값입니다.")
        @Size(max = 1000, message = "내용은 최대 1000자까지 입력 가능합니다.")
        String content
) {
}
