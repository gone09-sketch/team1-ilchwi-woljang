package com.team1ilchwiwoljang.domain.auth.dto.response;

import com.team1ilchwiwoljang.domain.member.entity.Member;

public record SignupResponse(
        Long memberId,
        String email,
        String name
) {

    public static SignupResponse from(Member member) {
        return new SignupResponse(
                member.getId(),
                member.getEmail(),
                member.getName()
        );
    }
}
