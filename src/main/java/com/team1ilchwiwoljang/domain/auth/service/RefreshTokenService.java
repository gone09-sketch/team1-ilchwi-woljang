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

    /**
     * 기존 Refresh Token을 제거한 뒤 새 Refresh Token을 발급합니다.
     * DB에는 RefreshToken 엔티티를 저장하고, 반환값은 실제 토큰 문자열입니다.
     */
    @Transactional
    public String createRefreshToken(Member member) {
        // 회원당 Refresh Token 1개만 유지합니다.
        refreshTokenRepository.deleteByMember(member);

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
    @Transactional
    public Member validateAndGetMember(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // revoked 가 true인 토큰은 이미 로그아웃 혹은 만료 처리된 토큰으로 봅니다.
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));

        // UTC 기준 현재 시각으로 가져옵니다.
        Instant now = Instant.now(clock);

        if (refreshToken.isExpired(now)) {
            refreshToken.revoke();
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        return refreshToken.getMember();
    }

    /**
     * 로그아웃 시 사용
     */
    @Transactional
    public void revoke(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .ifPresent(RefreshToken::revoke);
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