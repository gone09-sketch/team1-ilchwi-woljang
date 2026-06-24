package com.team1ilchwiwoljang.domain.inquiry.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.common.security.annotation.Auth;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryAnswerRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryCreateRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryAnswerResponse;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryCreateResponse;
import com.team1ilchwiwoljang.domain.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping("/api/members/inquiry")
    public ResponseEntity<ApiResponse<InquiryCreateResponse>> createInquiry(
            @Auth AuthMember authMember,
            @Valid @RequestBody InquiryCreateRequest request
    ) {

        InquiryCreateResponse response = inquiryService.createInquiry(authMember.memberId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    // TODO: 관리자 권한 검증 정책 수립 후 @PreAuthorize("hasRole('ADMIN')") 추가 고려
    @PostMapping("/api/admins/inquiry")
    public ResponseEntity<ApiResponse<InquiryAnswerResponse>> answerInquiry(
            @Auth AuthMember authMember,
            @Valid @RequestBody InquiryAnswerRequest request
    ) {
        InquiryAnswerResponse response = inquiryService.answerInquiry(authMember.memberId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
