package com.team1ilchwiwoljang.domain.inquiry.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryCreateRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryCreateResponse;
import com.team1ilchwiwoljang.domain.inquiry.service.InquiryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/members/inquiry")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<ApiResponse<InquiryCreateResponse>> createInquiry(
            @Valid @RequestBody InquiryCreateRequest request,
            Principal principal
    ) {
        InquiryCreateResponse response = inquiryService.createInquiry(principal.getName(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }
}
