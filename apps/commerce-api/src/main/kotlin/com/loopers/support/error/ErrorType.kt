package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class ErrorType(val status: HttpStatus, val code: String, val message: String) {
    /** 범용 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase, "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.reasonPhrase, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.reasonPhrase, "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.reasonPhrase, "이미 존재하는 리소스입니다."),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증에 실패했습니다."),

    /** Brand 도메인 */
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "BRAND_NOT_FOUND", "브랜드를 찾을 수 없습니다."),
    BRAND_INVALID_NAME(HttpStatus.BAD_REQUEST, "BRAND_INVALID_NAME", "브랜드명이 유효하지 않습니다."),
    BRAND_INVALID_STATUS(HttpStatus.BAD_REQUEST, "BRAND_INVALID_STATUS", "브랜드 상태가 유효하지 않습니다."),
    BRAND_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "BRAND_ALREADY_DELETED", "이미 삭제된 브랜드입니다."),

    /** Product 도메인 */
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "상품을 찾을 수 없습니다."),
    PRODUCT_INVALID_NAME(HttpStatus.BAD_REQUEST, "PRODUCT_INVALID_NAME", "상품명이 유효하지 않습니다."),
    PRODUCT_INVALID_PRICE(HttpStatus.BAD_REQUEST, "PRODUCT_INVALID_PRICE", "상품 가격이 유효하지 않습니다."),
    PRODUCT_INVALID_STATUS(HttpStatus.BAD_REQUEST, "PRODUCT_INVALID_STATUS", "상품 상태가 유효하지 않습니다."),
    PRODUCT_STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_STOCK_NOT_FOUND", "상품 재고를 찾을 수 없습니다."),
    PRODUCT_STOCK_INSUFFICIENT(HttpStatus.BAD_REQUEST, "PRODUCT_STOCK_INSUFFICIENT", "상품 재고가 부족합니다."),

    /** 공통 VO */
    INVALID_MONEY(HttpStatus.BAD_REQUEST, "INVALID_MONEY", "금액이 유효하지 않습니다."),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", "수량이 유효하지 않습니다."),

    /** Order 도메인 */
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다."),
    ORDER_INVALID_ITEMS(HttpStatus.BAD_REQUEST, "ORDER_INVALID_ITEMS", "주문 항목이 비어있습니다."),
    ORDER_INVALID_IDEMPOTENCY_KEY(HttpStatus.BAD_REQUEST, "ORDER_INVALID_IDEMPOTENCY_KEY", "멱등성 키가 유효하지 않습니다."),
    ORDER_IDEMPOTENCY_KEY_DUPLICATE(HttpStatus.CONFLICT, "ORDER_IDEMPOTENCY_KEY_DUPLICATE", "이미 처리된 주문 요청입니다."),

    /** User 도메인 */
    USER_DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "USER_DUPLICATE_LOGIN_ID", "이미 사용 중인 로그인 ID입니다."),
    USER_INVALID_LOGIN_ID(HttpStatus.BAD_REQUEST, "USER_INVALID_LOGIN_ID", "로그인 ID는 영문 대소문자와 숫자만 사용할 수 있습니다."),
    USER_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_INVALID_PASSWORD", "비밀번호는 영문, 숫자, 허용된 특수문자만 사용할 수 있습니다."),
    USER_INVALID_NAME(HttpStatus.BAD_REQUEST, "USER_INVALID_NAME", "이름은 한글만 입력할 수 있습니다."),
    USER_INVALID_EMAIL(HttpStatus.BAD_REQUEST, "USER_INVALID_EMAIL", "올바른 이메일 형식이 아닙니다."),
}
