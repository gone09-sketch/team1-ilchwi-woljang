package com.team1ilchwiwoljang.domain.auth.dto.response;

/**
 * NOTE: Refresh Token은 HttpOnly Cookie로 보내므로 응답dto에 포함하지 않습니다.
 */
public record LoginResponse(
        String accessToken
) {

    public static LoginResponse from(String accessToken) {
        return new LoginResponse(accessToken);
    }
}
