package com.team1ilchwiwoljang.domain.inquiry.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryAnswerRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryCreateRequest;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryAnswerResponse;
import com.team1ilchwiwoljang.domain.inquiry.dto.response.InquiryCreateResponse;
import com.team1ilchwiwoljang.domain.inquiry.entity.Inquiry;
import com.team1ilchwiwoljang.domain.inquiry.entity.InquiryStatus;
import com.team1ilchwiwoljang.domain.inquiry.repository.InquiryRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @InjectMocks
    private InquiryService inquiryService;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private MemberService memberService;

    @Test
    @DisplayName("가입된 이메일과 올바른 요청이 주어지면 문의 생성에 성공한다")
    void given_validRequest_whenCreateInquiry_thenSuccess() {
        // given
        String email = "test@example.com";
        InquiryCreateRequest request = new InquiryCreateRequest("문의 제목", "문의 내용");
        Member member = Member.create(email, "encodedPassword", "홍길동", "010-1234-5678");

        given(memberService.findByEmail(email)).willReturn(Optional.of(member));

        // when
        InquiryCreateResponse response = inquiryService.createInquiry(email, request);

        // then
        assertThat(response.title()).isEqualTo(request.title());
        assertThat(response.content()).isEqualTo(request.content());
        assertThat(response.status()).isEqualTo(InquiryStatus.WAITING);
        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    @DisplayName("존재하지 않는 회원의 이메일로 문의를 생성하면 MEMBER_NOT_FOUND 예외를 던진다")
    void given_nonExistentMember_whenCreateInquiry_thenThrowMemberNotFound() {
        // given
        String email = "nonexistent@example.com";
        InquiryCreateRequest request = new InquiryCreateRequest("문의 제목", "문의 내용");

        given(memberService.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiryService.createInquiry(email, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("올바른 답변 요청이 주어지면 문의 답변 등록에 성공한다")
    void given_validAnswerRequest_whenAnswerInquiry_thenSuccess() {
        // given
        Long inquiryId = 1L;
        Long adminId = 1L;
        InquiryAnswerRequest request = new InquiryAnswerRequest("답변 내용");

        Member member = Member.create("test@example.com", "password", "홍길동", "010-1234-5678");
        ReflectionTestUtils.setField(member, "id", 1L);

        Inquiry inquiry = Inquiry.create(member, "문의 제목", "문의 내용");
        ReflectionTestUtils.setField(inquiry, "id", inquiryId);

        given(inquiryRepository.findById(inquiryId)).willReturn(Optional.of(inquiry));

        // when
        InquiryAnswerResponse response = inquiryService.answerInquiry(inquiryId, adminId, request);

        // then
        assertThat(response.id()).isEqualTo(inquiryId);
        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.adminId()).isEqualTo(adminId);
        assertThat(response.answer()).isEqualTo("답변 내용");
        assertThat(response.status()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(response.answeredAt()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 문의에 답변을 등록하려 하면 INQUIRY_NOT_FOUND 예외를 던진다")
    void given_nonExistentInquiry_whenAnswerInquiry_thenThrowInquiryNotFound() {
        // given
        Long inquiryId = 999L;
        Long adminId = 1L;
        InquiryAnswerRequest request = new InquiryAnswerRequest("답변 내용");

        given(inquiryRepository.findById(inquiryId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inquiryService.answerInquiry(inquiryId, adminId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INQUIRY_NOT_FOUND);
    }
}
