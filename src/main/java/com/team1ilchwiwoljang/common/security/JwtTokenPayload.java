package com.team1ilchwiwoljang.common.security;

import com.team1ilchwiwoljang.domain.member.entity.MemberRole;

/**
 * 검증된 Access Token에서 꺼낸 인증 정보입니다.
 * JwtAuthenticationFilter가 JJWT의 Claims 타입을 직접 알 필요 없도록 분리합니다.
 */
public record JwtTokenPayload(
        Long memberId,
        MemberRole role
) {
}
