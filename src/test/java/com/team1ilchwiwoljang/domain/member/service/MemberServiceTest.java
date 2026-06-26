package com.team1ilchwiwoljang.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("회원 ID로 회원을 정상 조회한다")
    void given_existingMemberId_whenGetMember_thenReturnMember() {
        Long memberId = 1L;
        Member member = Member.create("member@example.com", "password", "member", "010-1234-5678");
        given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

        Member result = memberService.getMember(memberId);

        assertThat(result).isEqualTo(member);
    }

    @Test
    @DisplayName("존재하지 않는 회원 ID로 조회하면 MEMBER_NOT_FOUND 예외가 발생한다")
    void given_nonExistentMemberId_whenGetMember_thenThrowMemberNotFound() {
        Long memberId = 999L;
        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.getMember(memberId))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MEMBER_NOT_FOUND));
    }
}
