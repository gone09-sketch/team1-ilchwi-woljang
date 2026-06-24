package com.team1ilchwiwoljang.common.security;

import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final Long MEMBER_ID = 1L;
    private static final String ACCESS_TOKEN = "valid-access-token";

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private SecurityErrorResponseHandler securityErrorResponseHandler;

    @Mock
    private MemberService memberService;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("정상 회원의 Access Token이면 인증 정보를 저장하고 다음 필터로 넘긴다")
    void givenActiveMemberAccessToken_whenDoFilter_thenSetAuthenticationAndContinue() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);

        given(jwtTokenProvider.getMemberId(ACCESS_TOKEN)).willReturn(MEMBER_ID);
        given(memberService.findById(MEMBER_ID)).willReturn(Optional.of(mock(Member.class)));

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthMember(MEMBER_ID));

        verify(filterChain).doFilter(request, response);
        verify(securityErrorResponseHandler, never()).writeErrorResponse(any(), any());
    }

    @Test
    @DisplayName("탈퇴했거나 존재하지 않는 회원의 Access Token이면 401 응답을 반환한다")
    void givenDeletedMemberAccessToken_whenDoFilter_thenUnauthorized() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + ACCESS_TOKEN);

        given(jwtTokenProvider.getMemberId(ACCESS_TOKEN)).willReturn(MEMBER_ID);
        given(memberService.findById(MEMBER_ID)).willReturn(Optional.empty());

        // when
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(securityErrorResponseHandler).writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
