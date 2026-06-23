package com.team1ilchwiwoljang.common.response;

import lombok.Getter;

import java.util.List;

@Getter
public class ErrorResponse {

    private final boolean success = false;
    private final String code;
    private final String message;
    private final List<FieldError> errors;

    private ErrorResponse(String code, String message, List<FieldError> errors) {
        this.code = code;
        this.message = message;
        this.errors = errors;
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }

    public static ErrorResponse of(String code, String message, List<FieldError> errors) {
        return new ErrorResponse(code, message, errors);
    }

    @Getter
    public static class FieldError {

        private final String field;
        private final String message;

        public FieldError(String field, String message) {
            this.field = field;
            this.message = message;
        }
    }
}
