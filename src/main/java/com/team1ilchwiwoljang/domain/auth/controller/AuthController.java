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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${jwt.refresh-token-expiration}")
    private Duration refreshTokenExpiration;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

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

        servletResponse.addHeader(
                HttpHeaders.SET_COOKIE,
                createRefreshTokenCookie(result.refreshToken()).toString()
        );

        // Access Token만 응답 body로 내려줍니다.
        return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(result.accessToken())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse servletResponse
    ) {

        LoginResult result = authService.reissueToken(refreshToken);

        servletResponse.addHeader(
                HttpHeaders.SET_COOKIE,
                createRefreshTokenCookie(result.refreshToken()).toString()
        );

        return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(result.accessToken())));
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .sameSite("Strict")
                .maxAge(refreshTokenExpiration)
                .build();
    }
}
