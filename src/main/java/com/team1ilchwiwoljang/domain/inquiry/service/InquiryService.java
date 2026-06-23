package com.team1ilchwiwoljang.domain.inquiry.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryCreateRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryCreateResponse;
import com.team1ilchwiwoljang.domain.inquiry.entity.Inquiry;
import com.team1ilchwiwoljang.domain.inquiry.repository.InquiryRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public InquiryCreateResponse createInquiry(String email, InquiryCreateRequest request) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Inquiry inquiry = Inquiry.create(member, request.title(), request.content());
        inquiryRepository.save(inquiry);

        return InquiryCreateResponse.from(inquiry);
    }
}
