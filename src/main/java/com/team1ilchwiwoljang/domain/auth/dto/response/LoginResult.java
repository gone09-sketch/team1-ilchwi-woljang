package com.team1ilchwiwoljang.domain.auth.dto.response;

/**
 * 클라이언트 응답 JSON 전용이 아니라 서버 내부 전달용입니다.
 */
public record LoginResult(

        String accessToken,
        String refreshToken
) {
}