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

    public JwtTokenPayload parseAccessToken(String token) {
        // JWT 서명 검증과 디코딩은 비용이 있는 작업이므로 한 번만 수행합니다.
        Claims claims = parseClaims(token);

        return new JwtTokenPayload(
                getMemberId(claims),
                getRole(claims)
        );
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Long getMemberId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    private MemberRole getRole(Claims claims) {
        String roleClaim = claims.get("role", String.class);

        if (roleClaim == null || roleClaim.isBlank()) {
            throw new JwtException("Invalid JWT");
        }

        try {
            return MemberRole.valueOf(roleClaim);
        } catch (IllegalArgumentException e) {
            throw new JwtException("Invalid JWT", e);
        }
    }
}