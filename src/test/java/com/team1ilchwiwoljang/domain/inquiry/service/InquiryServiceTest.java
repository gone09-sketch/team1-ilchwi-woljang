package com.team1ilchwiwoljang.domain.inquiry.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.response.PageResponse;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryAnswerRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryCreateRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryAdminResponse;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryAnswerResponse;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryCreateResponse;
import com.team1ilchwiwoljang.domain.inquiry.entity.Inquiry;
import com.team1ilchwiwoljang.domain.inquiry.entity.InquiryStatus;
import com.team1ilchwiwoljang.domain.inquiry.repository.InquiryRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @InjectMocks
    private InquiryService inquiryService;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private Clock clock;

    @Test
    @DisplayName("관리자 문의 목록 조회 시 최신순 페이지를 응답 DTO로 변환한다")
    void given_pageable_whenGetAdminInquiries_thenReturnPageResponse() {
        // given
        Long memberId = 1L;
        Member member = Member.create("test@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);

        Inquiry inquiry = Inquiry.create(member, "배송 문의", "언제 배송되나요?");
        ReflectionTestUtils.setField(inquiry, "id", 10L);
        ReflectionTestUtils.setField(inquiry, "createdAt", LocalDateTime.of(2026, 6, 25, 14, 30));

        Pageable pageable = PageRequest.of(0, 20);
        given(inquiryRepository.findAllByOrderByCreatedAtDesc(pageable))
                .willReturn(new PageImpl<>(List.of(inquiry), pageable, 1));

        // when
        PageResponse<InquiryAdminResponse> response = inquiryService.getAdminInquiries(pageable);

        // then
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(10L);
        assertThat(response.content().get(0).memberId()).isEqualTo(memberId);
        assertThat(response.content().get(0).title()).isEqualTo("배송 문의");
        assertThat(response.content().get(0).status()).isEqualTo(InquiryStatus.WAITING);
    }

    @Test
    @DisplayName("회원 ID와 올바른 요청이 주어지면 문의 생성에 성공한다")
    void given_memberIdAndValidRequest_whenCreateInquiry_thenSuccess() {
        // given
        Long memberId = 1L;

        // 클라이언트가 보낸 문의 생성 요청입니다.
        InquiryCreateRequest request = new InquiryCreateRequest("문의 제목", "문의 내용");

        // Member 엔티티를 실제로 만들지 않고 mock 객체로 대체합니다.
        Member member = mock(Member.class);

        // InquiryCreateResponse.from() 내부에서 member.getId()를 사용하므로 id만 stub 처리합니다.
        given(member.getId()).willReturn(memberId);

        // memberId로 회원 조회 시 mock Member가 반환되도록 설정합니다.
        given(memberService.findById(memberId)).willReturn(Optional.of(member));

        // when
        InquiryCreateResponse response = inquiryService.createInquiry(memberId, request);

        // then
        assertThat(response.memberId()).isEqualTo(memberId);
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.content()).isEqualTo(request.content());
        assertThat(response.status()).isEqualTo(InquiryStatus.WAITING);

        // Inquiry 저장이 호출됐는지 확인합니다.
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 문의를 생성하면 MEMBER_NOT_FOUND 예외를 던진다")
    void given_nonExistentMemberId_whenCreateInquiry_thenThrowMemberNotFound() {
        // given
        Long memberId = 999L;

        // 클라이언트가 보낸 문의 생성 요청입니다.
        InquiryCreateRequest request = new InquiryCreateRequest("문의 제목", "문의 내용");

        // memberId로 회원을 찾지 못한 상황입니다.
        given(memberService.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiryService.createInquiry(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("올바른 답변 요청이 주어지면 문의 답변 등록에 성공한다")
    void given_validAnswerRequest_whenAnswerInquiry_thenSuccess() {
        // given
        Long inquiryId = 1L;
        Long adminId = 1L;
        InquiryAnswerRequest request = new InquiryAnswerRequest(inquiryId, "답변 내용");

        Member member = Member.create("test@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", 1L);

        Inquiry inquiry = Inquiry.create(member, "문의 제목", "문의 내용");
        ReflectionTestUtils.setField(inquiry, "id", inquiryId);

        Member admin = Member.create("admin@example.com", "password", "관리자", "010-9999-9999");
        ReflectionTestUtils.setField(admin, "id", adminId);
        ReflectionTestUtils.setField(admin, "role", MemberRole.ADMIN);

        given(memberService.findById(adminId)).willReturn(Optional.of(admin));
        given(inquiryRepository.findById(inquiryId)).willReturn(Optional.of(inquiry));

        Instant fixedInstant = Instant.parse("2026-06-24T08:00:00Z");
        given(clock.instant()).willReturn(fixedInstant);
        given(clock.getZone()).willReturn(ZoneId.of("UTC"));

        // when
        InquiryAnswerResponse response = inquiryService.answerInquiry(adminId, request);

        // then
        assertThat(response.id()).isEqualTo(inquiryId);
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.adminId()).isEqualTo(adminId);
        assertThat(response.answer()).isEqualTo("답변 내용");
        assertThat(response.status()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(response.answeredAt()).isEqualTo(LocalDateTime.of(2026, 6, 24, 8, 0, 0));
    }

    @Test
    @DisplayName("존재하지 않는 문의에 답변을 등록하려 하면 INQUIRY_NOT_FOUND 예외를 던진다")
    void given_nonExistentInquiry_whenAnswerInquiry_thenThrowInquiryNotFound() {
        // given
        Long inquiryId = 999L;
        Long adminId = 1L;
        InquiryAnswerRequest request = new InquiryAnswerRequest(inquiryId, "답변 내용");

        Member admin = Member.create("admin@example.com", "password", "관리자", "010-9999-9999");
        ReflectionTestUtils.setField(admin, "id", adminId);
        ReflectionTestUtils.setField(admin, "role", MemberRole.ADMIN);

        given(memberService.findById(adminId)).willReturn(Optional.of(admin));
        given(inquiryRepository.findById(inquiryId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiryService.answerInquiry(adminId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INQUIRY_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 답변 완료된 문의에 답변을 등록하려 하면 ALREADY_ANSWERED_INQUIRY 예외를 던진다")
    void given_alreadyAnsweredInquiry_whenAnswerInquiry_thenThrowAlreadyAnsweredInquiry() {
        // given
        Long inquiryId = 1L;
        Long adminId = 1L;
        InquiryAnswerRequest request = new InquiryAnswerRequest(inquiryId, "답변 내용");

        Member member = Member.create("test@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", 1L);

        Inquiry inquiry = Inquiry.create(member, "문의 제목", "문의 내용");
        ReflectionTestUtils.setField(inquiry, "id", inquiryId);
        inquiry.answer(adminId, "기존 답변 내용", LocalDateTime.now());

        Member admin = Member.create("admin@example.com", "password", "관리자", "010-9999-9999");
        ReflectionTestUtils.setField(admin, "id", adminId);
        ReflectionTestUtils.setField(admin, "role", MemberRole.ADMIN);

        given(memberService.findById(adminId)).willReturn(Optional.of(admin));
        given(inquiryRepository.findById(inquiryId)).willReturn(Optional.of(inquiry));

        // when & then
        assertThatThrownBy(() -> inquiryService.answerInquiry(adminId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_ANSWERED_INQUIRY);
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 답변을 등록하려 하면 MEMBER_NOT_FOUND 예외를 던진다")
    void given_nonExistentMemberId_whenAnswerInquiry_thenThrowMemberNotFound() {
        // given
        Long inquiryId = 1L;
        Long adminId = 999L;
        InquiryAnswerRequest request = new InquiryAnswerRequest(inquiryId, "답변 내용");

        given(memberService.findById(adminId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiryService.answerInquiry(adminId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("관리자 권한이 없는 회원이 답변을 등록하려 하면 FORBIDDEN 예외를 던진다")
    void given_nonAdminUser_whenAnswerInquiry_thenThrowForbidden() {
        // given
        Long inquiryId = 1L;
        Long memberId = 1L;
        InquiryAnswerRequest request = new InquiryAnswerRequest(inquiryId, "답변 내용");

        Member member = Member.create("user@example.com", "password", "일반회원", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", memberId);
        ReflectionTestUtils.setField(member, "role", MemberRole.MEMBER);

        given(memberService.findById(memberId)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> inquiryService.answerInquiry(memberId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}
