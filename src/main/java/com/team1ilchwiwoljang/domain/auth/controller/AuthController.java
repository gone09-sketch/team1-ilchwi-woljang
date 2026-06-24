package com.team1ilchwiwoljang.domain.auth.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.domain.auth.dto.request.LoginRequest;
import com.team1ilchwiwoljang.domain.auth.dto.request.SignupRequest;
import com.team1ilchwiwoljang.domain.auth.dto.response.LoginResponse;
import com.team1ilchwiwoljang.domain.auth.dto.response.LoginResult;
import com.team1ilchwiwoljang.domain.auth.dto.response.SignupResponse;
import com.team1ilchwiwoljang.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        SignupResponse response = authService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,

            // Refresh Token을 Cookie로 내려보내기 위해 servletResponse를 받습니다.
            HttpServletResponse servletResponse
    ) {

        LoginResult result = authService.login(request);

        // Refresh Token은 JavaScript에서 읽지 못하도록 HttpOnly Cookie로 내려보냅니다.
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", result.refreshToken())
                // JavaScript로 쿠키를 읽지 못하게 막습니다.
                .httpOnly(true)

                // NOTE: 실서비스 HTTPS 환경에서는 true 로 바꾸기
                .secure(false)
                .path("/api/auth")
                .sameSite("Strict")
                .maxAge(Duration.ofDays(14))

                // Cookie 문자열로 변환합니다.
                .build();

        // 응답 헤더에 Set-Cookie를 추가합니다.
        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

        // Access Token만 응답 body로 내려줍니다.
        return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(result.accessToken())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {

        LoginResponse response = authService.reissueAccessToken(refreshToken);

        // 새 Access Token을 응답 body로 반환합니다.
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
