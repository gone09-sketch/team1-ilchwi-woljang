package com.team1ilchwiwoljang.common.exception.handler;

import com.team1ilchwiwoljang.common.exception.BusinessException;
import com.team1ilchwiwoljang.common.exception.ErrorCode;
import com.team1ilchwiwoljang.common.response.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.of(errorCode.name(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {
        ErrorCode errorCode = ErrorCode.CART_ITEM_QUANTITY_EXCEEDED;
        ErrorResponse response = ErrorResponse.of(errorCode.name(), errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();

        List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(fieldError -> new ErrorResponse.FieldError(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        ErrorResponse response = ErrorResponse.of(errorCode.name(), errorCode.getMessage(), fieldErrors);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            org.springframework.web.bind.MissingServletRequestParameterException e) {
        ErrorCode errorCode = ErrorCode.VALIDATION_FAILED;
        ErrorResponse response = ErrorResponse.of(
                errorCode.name(),
                "필수 쿼리 파라미터 '" + e.getParameterName() + "'가 누락되었습니다."
        );
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        ErrorResponse response = ErrorResponse.of("INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");
        return ResponseEntity.internalServerError().body(response);
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handlePessimisticLockException(
            PessimisticLockingFailureException e
    ){
        ErrorCode errorCode = ErrorCode.ORDER_CONFLICT;
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(errorCode.name(), errorCode.getMessage()));
    }

    /**
     * RequestParam 또는 PathVariable 타입 변환에 실패했을 때 처리합니다.
     * 클라이언트가 잘못된 요청 값을 보낸 상황이므로
     * 500 INTERNAL_SERVER_ERROR가 아니라
     * 400 BAD_REQUEST 계열의 INVALID_ENUM_VALUE로 응답합니다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        /*
         * status=BAD_STATUS처럼 허용되지 않은 enum 값이 들어온 경우
         * INVALID_ENUM_VALUE를 사용해 클라이언트 요청 오류로 내려줍니다.
         */
        ErrorCode errorCode = ErrorCode.INVALID_ENUM_VALUE;

        ErrorResponse response = ErrorResponse.of(
                errorCode.name(),
                errorCode.getMessage()
        );

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }
}
