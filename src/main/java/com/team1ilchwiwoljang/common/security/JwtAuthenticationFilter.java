package com.team1ilchwiwoljang.common.security;

import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.security.auth.AuthMember;
import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import com.team1ilchwiwoljang.domain.member.service.MemberService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SecurityException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityErrorResponseHandler securityErrorResponseHandler;
    private final MemberService memberService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // HTTP Authorization 헤더 값을 꺼냅니다.
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        /*
         * 회원가입, 로그인 같은 공개 API는 토큰 없이도 통과해야 하기 때문에
         * 토큰이 없는 요청을 여기에서 실패 처리 하지 않습니다.
         */
        if (authorizationHeader == null) {
            filterChain.doFilter(request, response);
            return;
        }

        /*
         * Authorization 헤더가 있는데 "Bearer " 형식이 아니면 잘못된 인증 요청이며,
         * 예외를 던지지 않고 공통 401 응답을 내려서 프로젝트 응답 형식을 통일합니다.
         */
        if (!authorizationHeader.startsWith("Bearer ")) {
            log.warn("Authorization header does not start with Bearer");

            sendUnauthorizedResponse(response);
            return;
        }

        // "Bearer " 뒤의 실제 JWT 문자열만 잘라냅니다.
        String accessToken = authorizationHeader.substring(7).trim();

        // "Bearer "만 있고 실제 토큰이 비어 있는 경우
        if (accessToken.isBlank()) {
            log.warn("JWT token is blank");

            sendUnauthorizedResponse(response);
            return;
        }

        try {
            // Access Token을 한 번만 검증/파싱해서 인증에 필요한 값을 꺼냅니다.
            JwtTokenPayload tokenPayload = jwtTokenProvider.parseAccessToken(accessToken);

            Long memberId = tokenPayload.memberId();
            MemberRole role = tokenPayload.role();

            if (!memberService.existsActiveMember(memberId)) {
                log.warn("Authenticated member does not exist or is deleted");

                sendUnauthorizedResponse(response);
                return;
            }

            // Controller에서 @Auth AuthMember로 받을 인증 사용자 객체를 만듭니다.
            AuthMember authMember = new AuthMember(memberId, role);

            // hasRole("ADMIN")은 내부적으로 ROLE_ADMIN 권한을 찾습니다.
            SimpleGrantedAuthority authority =
                    new SimpleGrantedAuthority("ROLE_" + role.name());

            /*
             * Spring Security가 관리하는 인증 객체를 생성합니다.
             * 첫 번째 값 principal: 인증된 사용자 정보
             * 두 번째 값 credentials: 비밀번호 같은 인증 수단, JWT 방식에서는 null
             * 세 번째 값 authorities: 권한 목록
             */
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            authMember,
                            null,
                            List.of(authority)
                    );

            // SecurityContext에 인증 정보를 저장합니다.
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException e) {
            // Access Token이 만료된 경우
            log.warn("Expired JWT token");

            sendUnauthorizedResponse(response);
            return;

        } catch (MalformedJwtException | UnsupportedJwtException | SecurityException e) {
            // JWT 형식이 깨졌거나, 지원하지 않는 JWT이거나, 서명이 잘못된 경우
            log.warn("Invalid JWT token");

            sendUnauthorizedResponse(response);
            return;

        } catch (IllegalArgumentException e) {
            // JWT subject가 비어 있거나 "abc"처럼 memberId로 바꿀 수 없는 값인 경우
            log.warn("Invalid JWT claims");

            sendUnauthorizedResponse(response);
            return;

        } catch (JwtException e) {
            // 위에서 구체적으로 잡지 못한 JWT 관련 예외를 마지막으로 처리
            log.warn("JWT authentication failed");

            sendUnauthorizedResponse(response);
            return;
        }

        // JWT 인증이 성공했으므로 다음 필터 또는 Controller로 요청을 넘깁니다.
        filterChain.doFilter(request, response);
    }

    /**
     * JWT 인증 실패 응답을 공통 ErrorResponse 형식으로 내려주는 메서드입니다.
     */
    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        // 잘못된 인증 정보가 SecurityContext에 남지 않도록 비웁니다.
        SecurityContextHolder.clearContext();

        if (response.isCommitted()) {
            return;
        }

        securityErrorResponseHandler.writeErrorResponse(response, ErrorCode.UNAUTHORIZED);
    }
}
