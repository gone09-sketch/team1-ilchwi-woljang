package com.team1ilchwiwoljang.common.security.auth;

import com.team1ilchwiwoljang.domain.member.entity.MemberRole;

public record AuthMember(
        Long memberId,
        MemberRole role
) {
}