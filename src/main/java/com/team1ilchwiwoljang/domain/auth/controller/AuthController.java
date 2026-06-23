package com.team1ilchwiwoljang.domain.auth.controller;

import com.team1ilchwiwoljang.common.response.ApiResponse;
import com.team1ilchwiwoljang.domain.auth.dto.request.SignupRequest;
import com.team1ilchwiwoljang.domain.auth.dto.response.SignupResponse;
import com.team1ilchwiwoljang.domain.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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


}
