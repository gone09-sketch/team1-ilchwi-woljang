package com.team1ilchwiwoljang.domain.auth.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.domain.auth.dto.request.LoginRequest;
import com.team1ilchwiwoljang.domain.auth.dto.response.LoginResult;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String EMAIL = "test@example.com";
    private static final String RAW_PASSWORD = "Password123!";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String NEW_REFRESH_TOKEN = "new-refresh-token";

    @InjectMocks
    private AuthService authService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("가입된 이메일과 올바른 비밀번호로 로그인하면 Access Token과 Refresh Token을 발급한다")
    void givenValidCredentials_whenLogin_thenIssueTokens() {
        // given
        LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
        Member member = mock(Member.class);

        given(memberRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(member));
        given(member.getPassword()).willReturn(ENCODED_PASSWORD);
        given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(true);
        given(member.getId()).willReturn(MEMBER_ID);
        given(jwtTokenProvider.createAccessToken(MEMBER_ID)).willReturn(ACCESS_TOKEN);
        given(refreshTokenService.createRefreshToken(member)).willReturn(REFRESH_TOKEN);

        // when
        LoginResult result = authService.login(request);

        // then
        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
        verify(refreshTokenService).revokeAllByMember(member);
    }

    @Test
    @DisplayName("가입되지 않은 이메일로 로그인하면 UNAUTHORIZED 예외를 던진다")
    void givenUnknownEmail_whenLogin_thenThrowUnauthorized() {
        // given
        LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
        given(memberRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(refreshTokenService, never()).revokeAllByMember(any());
        verify(jwtTokenProvider, never()).createAccessToken(anyLong());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 UNAUTHORIZED 예외를 던지고 토큰을 발급하지 않는다")
    void givenWrongPassword_whenLogin_thenThrowUnauthorized() {
        // given
        LoginRequest request = new LoginRequest(EMAIL, RAW_PASSWORD);
        Member member = mock(Member.class);

        given(memberRepository.findByEmailAndDeletedAtIsNull(EMAIL)).willReturn(Optional.of(member));
        given(member.getPassword()).willReturn(ENCODED_PASSWORD);
        given(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

        verify(jwtTokenProvider, never()).createAccessToken(anyLong());
        verify(refreshTokenService, never()).revokeAllByMember(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    @DisplayName("유효한 Refresh Token이면 Access Token과 Refresh Token을 재발급한다")
    void givenValidRefreshToken_whenReissueToken_thenReturnNewTokens() {
        // given
        Member member = mock(Member.class);

        given(refreshTokenService.validateAndGetMember(REFRESH_TOKEN)).willReturn(member);
        given(member.getId()).willReturn(MEMBER_ID);
        given(jwtTokenProvider.createAccessToken(MEMBER_ID)).willReturn(ACCESS_TOKEN);
        given(refreshTokenService.createRefreshToken(member)).willReturn(NEW_REFRESH_TOKEN);

        // when
        LoginResult result = authService.reissueToken(REFRESH_TOKEN);

        // then
        assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(NEW_REFRESH_TOKEN);
        verify(refreshTokenService).revokeAllByMember(member);
    }

    @Test
    @DisplayName("유효하지 않은 Refresh Token이면 UNAUTHORIZED 예외를 그대로 전달한다")
    void givenInvalidRefreshToken_whenReissueToken_thenThrowUnauthorized() {
        // given
        given(refreshTokenService.validateAndGetMember(REFRESH_TOKEN))
                .willThrow(new BusinessException(ErrorCode.UNAUTHORIZED));

        // when & then
        assertThatThrownBy(() -> authService.reissueToken(REFRESH_TOKEN))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);

        verify(jwtTokenProvider, never()).createAccessToken(anyLong());
        verify(refreshTokenService, never()).revokeAllByMember(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }
}
