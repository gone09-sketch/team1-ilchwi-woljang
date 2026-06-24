package com.team1ilchwiwoljang.domain.member.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.auth.service.RefreshTokenService;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    private static final Long MEMBER_ID = 1L;

    @InjectMocks
    private MemberService memberService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("회원 권한이 변경되면 기존 Refresh Token을 모두 폐기한다")
    void givenDifferentRole_whenChangeRole_thenRevokeRefreshTokens() {
        // given
        Member member = mock(Member.class);
        given(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).willReturn(Optional.of(member));
        given(member.getRole()).willReturn(MemberRole.MEMBER);

        // when
        memberService.changeRole(MEMBER_ID, MemberRole.ADMIN);

        // then
        verify(member).changeRole(MemberRole.ADMIN);
        verify(refreshTokenService).revokeAllByMember(member);
    }

    @Test
    @DisplayName("회원 권한이 동일하면 Refresh Token을 폐기하지 않는다")
    void givenSameRole_whenChangeRole_thenDoNothing() {
        // given
        Member member = mock(Member.class);
        given(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).willReturn(Optional.of(member));
        given(member.getRole()).willReturn(MemberRole.MEMBER);

        // when
        memberService.changeRole(MEMBER_ID, MemberRole.MEMBER);

        // then
        verify(member, never()).changeRole(any());
        verify(refreshTokenService, never()).revokeAllByMember(any());
    }

    @Test
    @DisplayName("존재하지 않는 회원의 권한을 변경하면 MEMBER_NOT_FOUND 예외가 발생한다")
    void givenUnknownMember_whenChangeRole_thenThrowMemberNotFound() {
        // given
        given(memberRepository.findByIdAndDeletedAtIsNull(MEMBER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.changeRole(MEMBER_ID, MemberRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);

        verify(refreshTokenService, never()).revokeAllByMember(any());
    }
}
