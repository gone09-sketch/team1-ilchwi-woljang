package com.team1ilchwiwoljang.domain.auth.service;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.domain.auth.entity.RefreshToken;
import com.team1ilchwiwoljang.domain.auth.repository.RefreshTokenRepository;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    // UTC 기준 Clock을 주입받아 사용하여 서버 시간대 차이를 줄입니다.
    private final Clock clock;

    // Random보다 보안 목적의 난수 생성을 위한 SecureRandom을 사용합니다.
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-token-expiration}")
    private Duration refreshTokenExpiration;

    @Transactional
    public String createRefreshToken(Member member) {
        String token = generateRandomToken();

        // UTC 기준 현재 시각에서 만료 시간을 계산합니다.
        Instant expiresAt = Instant.now(clock)
                .plus(refreshTokenExpiration);

        RefreshToken refreshToken = RefreshToken.createRefreshToken(member, token, expiresAt);

        refreshTokenRepository.save(refreshToken);

        return token;
    }

    /**
     * Refresh Token이 유효한지 검증하고 유효하다면 해당 토큰의 회원정보를 반환합니다.
     * Access Token 재발급 시 사용
     */
    @Transactional(readOnly = true)
    public Member validateAndGetMember(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        if (refreshToken.isExpired(Instant.now(clock))) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return refreshToken.getMember();
    }

    @Transactional
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        refreshTokenRepository.revokeByToken(token);
    }

    @Transactional
    public void revokeAllByMember(Member member) {
        refreshTokenRepository.revokeAllByMemberId(member.getId());
    }

    /**
     * 랜덤 문자열 Refresh Token 생성 메서드
     */
    private String generateRandomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        // 쿠키나 URL에서 다루기 쉬운 Base64 URL-safe 문자열로 변환합니다.
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}