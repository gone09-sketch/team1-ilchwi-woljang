package com.team1ilchwiwoljang.domain.inquiry.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryAnswerRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryCreateRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryAnswerResponse;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryCreateResponse;
import com.team1ilchwiwoljang.domain.inquiry.entity.Inquiry;
import com.team1ilchwiwoljang.domain.inquiry.repository.InquiryRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final MemberService memberService;
    private final Clock clock;

    @Transactional
    public InquiryCreateResponse createInquiry(Long memberId, InquiryCreateRequest request) {
        Member member = memberService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        Inquiry inquiry = Inquiry.create(member, request.title(), request.content());
        inquiryRepository.save(inquiry);

        return InquiryCreateResponse.from(inquiry);
    }

    @Transactional
    public InquiryAnswerResponse answerInquiry(Long memberId, InquiryAnswerRequest request) {
        Member member = memberService.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getRole() != MemberRole.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        Inquiry inquiry = inquiryRepository.findById(request.inquiryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));

        if (inquiry.isAnswered()) {
            throw new BusinessException(ErrorCode.ALREADY_ANSWERED_INQUIRY);
        }

        inquiry.answer(member.getId(), request.answer(), LocalDateTime.now(clock));

        return InquiryAnswerResponse.from(inquiry);
    }
}
