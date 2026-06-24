package com.team1ilchwiwoljang.domain.inquiry.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.inquiry.dto.request.InquiryCreateRequest;
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
}
