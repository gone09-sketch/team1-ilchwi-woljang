package com.team1ilchwiwoljang.domain.auth.entity;

import com.team1ilchwiwoljang.common.entity.BaseEntity;
import com.team1ilchwiwoljang.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    // 만료 시각은 서버 위치와 무관한 UTC 기준 Instant 로 저장합니다.
    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    public static RefreshToken createRefreshToken(
            Member member,
            String token,
            Instant expiresAt
    ) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.member = member;
        refreshToken.token = token;
        refreshToken.expiresAt = expiresAt;
        refreshToken.revoked = false;
        return refreshToken;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }
}
