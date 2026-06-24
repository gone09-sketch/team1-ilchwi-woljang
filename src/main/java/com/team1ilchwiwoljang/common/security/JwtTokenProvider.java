package com.team1ilchwiwoljang.common.security;

import com.team1ilchwiwoljang.domain.member.entity.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final Duration accessTokenExpiration;
    private final Clock clock;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") Duration accessTokenExpiration,
            Clock clock
    ) {
        // 문자열 secret을 바이트 배열로 바꾼 뒤 JWT 서명용 SecretKey로 변환합니다.
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.clock = clock;
    }

    public String createAccessToken(Long memberId, MemberRole role) {
        // 토큰이 발급된 현재 시각
        Instant now = clock.instant();

        // Access Token 만료 시각 = 현재 시각 + 설정된 만료 시간
        Instant expiration = now.plus(accessTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public Long getMemberId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.valueOf(claims.getSubject());
    }

    public MemberRole getRole(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // JWT 안의 role claim 값을 꺼냅니다.
        String roleClaim = claims.get("role", String.class);

        // 외부에 상세 원인을 노출하지 않기 위해 메시지는 일반적으로 둡니다.
        if (roleClaim == null || roleClaim.isBlank()) {
            throw new JwtException("Invalid JWT");
        }

        try {
            return MemberRole.valueOf(roleClaim);
        } catch (IllegalArgumentException e) {
            // 알 수 없는 role 값도 동일하게 인증 실패로 처리합니다.
            throw new JwtException("Invalid JWT", e);
        }
    }
}