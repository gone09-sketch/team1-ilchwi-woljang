package com.team1ilchwiwoljang.domain.auth.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.security.JwtTokenProvider;
import com.team1ilchwiwoljang.domain.auth.dto.request.LoginRequest;
import com.team1ilchwiwoljang.domain.auth.dto.request.SignupRequest;
import com.team1ilchwiwoljang.domain.auth.dto.response.LoginResponse;
import com.team1ilchwiwoljang.domain.auth.dto.response.LoginResult;
import com.team1ilchwiwoljang.domain.auth.dto.response.SignupResponse;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import com.team1ilchwiwoljang.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        Member member = Member.create(request.email(), encodedPassword, request.name(), request.phone());

        Member savedMember = memberRepository.save(member);

        return SignupResponse.from(savedMember);
    }

    @Transactional
    public LoginResult login(LoginRequest request) {
        Member member = memberRepository.findByEmailAndDeletedAtIsNull(request.email())
                // 가입된 이메일인지 공격자가 추측하지 못하게 하기 위해 MEMBER_NOT_FOUND가 아닌 UNAUTHORIZED 사용
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            // 비밀번호가 틀려도 동일하게 UNAUTHORIZED 반환
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 기존 활성 토큰을 먼저 폐기 후, 새 토큰을 발급합니다.
        refreshTokenService.revokeAllByMember(member);

        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = refreshTokenService.createRefreshToken(member);

        // Controller에서 Access Token은 body로, Refresh Token은 Cookie로 내려보낼 수 있게 반환
        return new LoginResult(accessToken, refreshToken);
    }

    /**
     * Refresh Token이 유효하면 새 Access Token을 발급하고
     * 기존 Refresh Token 폐기 후 재발급
     */
    @Transactional
    public LoginResult reissueToken(String refreshToken) {
        Member member = refreshTokenService.validateAndGetMember(refreshToken);

        refreshTokenService.revokeAllByMember(member);

        String newAccessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String newRefreshToken = refreshTokenService.createRefreshToken(member);

        return new LoginResult(newAccessToken, newRefreshToken);
    }
}
