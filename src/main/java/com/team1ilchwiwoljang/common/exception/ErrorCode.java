package com.team1ilchwiwoljang.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // global
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 본문, 쿼리 파라미터, 경로 변수 검증에 실패했습니다."),
    INVALID_ENUM_VALUE(HttpStatus.BAD_REQUEST, "허용하지 않는 Enum 값입니다."),

    // auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),


    // product
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),

    // cart
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "수량은 1개 이상이어야 합니다."),
    CART_ITEM_QUANTITY_EXCEEDED(HttpStatus.BAD_REQUEST, "장바구니에 담을 수 있는 수량이 재고를 초과했습니다."),

    // category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),


    // inquiry
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "문의를 찾을 수 없습니다."),
    ALREADY_ANSWERED_INQUIRY(HttpStatus.BAD_REQUEST, "이미 답변 완료된 문의입니다."),

    // order
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "주문을 찾을 수 없습니다."),
    INVALID_ORDER_STATUS(HttpStatus.BAD_REQUEST, "변경할 수 없는 주문 상태입니다."),
    OUT_OF_STOCK(HttpStatus.BAD_REQUEST, "재고가 부족합니다.");

    private final HttpStatus status;
    private final String message;
}
