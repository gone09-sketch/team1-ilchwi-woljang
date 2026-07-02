package com.team1ilchwiwoljang.domain.inquiry.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InquiryAnswerRequest(
        @NotNull(message = "문의 ID는 필수 입력 값입니다.")
        Long inquiryId,

        @NotBlank(message = "답변 내용은 필수 입력 값입니다.")
        @Size(max = 1000, message = "답변 내용은 최대 1000자까지 입력 가능합니다.")
        String answer
) {
}
