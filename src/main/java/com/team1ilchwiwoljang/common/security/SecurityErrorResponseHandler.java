package com.team1ilchwiwoljang.common.security;

import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.response.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseHandler {

    private final ObjectMapper objectMapper;

    public void writeErrorResponse(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {

        // 이미 응답이 내려간 상태라면 body를 다시 쓰지 않습니다.
        if (response.isCommitted()) {
            return;
        }

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode.name(),
                errorCode.getMessage()
        );

        // Security 예외는 Controller까지 가지 않으므로 여기서 직접 HTTP 상태 코드를 지정합니다.
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // ErrorResponse 객체를 JSON으로 변환해서 응답 body에 씁니다.
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}