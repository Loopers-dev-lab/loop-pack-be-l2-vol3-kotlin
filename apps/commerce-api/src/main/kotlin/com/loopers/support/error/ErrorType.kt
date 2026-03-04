package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class ErrorType(val status: HttpStatus, val code: String, val message: String) {
    /** 범용 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase, "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.reasonPhrase, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.reasonPhrase, "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.reasonPhrase, "이미 존재하는 리소스입니다."),

    /** 회원 도메인 에러 */
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "E001", "이미 존재하는 로그인 ID입니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "E002", "비밀번호 형식이 올바르지 않습니다."),
    INVALID_LOGIN_ID_FORMAT(HttpStatus.BAD_REQUEST, "E003", "로그인 ID는 영문과 숫자만 허용합니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "E004", "이메일 형식이 올바르지 않습니다."),
    PASSWORD_CONTAINS_BIRTHDATE(HttpStatus.BAD_REQUEST, "E005", "비밀번호에 생년월일을 포함할 수 없습니다."),
    SAME_PASSWORD_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "E006", "현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "E007", "회원을 찾을 수 없습니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "E008", "인증에 실패했습니다."),
    INVALID_NAME_FORMAT(HttpStatus.BAD_REQUEST, "E009", "이름 형식이 올바르지 않습니다."),
    INVALID_BIRTHDATE_FORMAT(HttpStatus.BAD_REQUEST, "E010", "생년월일 형식이 올바르지 않습니다."),

    /** 브랜드 도메인 에러 */
    DUPLICATE_BRAND_NAME(HttpStatus.CONFLICT, "E101", "이미 존재하는 브랜드명입니다."),
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "E102", "브랜드를 찾을 수 없습니다."),
    INVALID_BRAND_NAME_FORMAT(HttpStatus.BAD_REQUEST, "E103", "브랜드명 형식이 올바르지 않습니다."),
    BRAND_HAS_ACTIVE_PRODUCTS(HttpStatus.CONFLICT, "E104", "판매 중인 상품이 있는 브랜드는 비활성화할 수 없습니다."),
    BRAND_ALREADY_INACTIVE(HttpStatus.CONFLICT, "E105", "이미 비활성화된 브랜드입니다."),
    BRAND_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "E106", "활성 상태가 아닌 브랜드입니다."),

    /** 상품 도메인 에러 */
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "E201", "상품을 찾을 수 없습니다."),
    INVALID_PRODUCT_NAME_FORMAT(HttpStatus.BAD_REQUEST, "E202", "상품명 형식이 올바르지 않습니다."),
    INVALID_PRODUCT_PRICE(HttpStatus.BAD_REQUEST, "E203", "상품 가격이 올바르지 않습니다."),
    INVALID_PRODUCT_DESCRIPTION_LENGTH(HttpStatus.BAD_REQUEST, "E204", "상품 설명 길이가 올바르지 않습니다."),
    PRODUCT_ALREADY_STOP_SELLING(HttpStatus.CONFLICT, "E205", "이미 판매 중지된 상품입니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "E206", "재고가 부족합니다."),

    /** 좋아요 도메인 에러 */
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "E301", "좋아요를 찾을 수 없습니다."),
    ALREADY_LIKED(HttpStatus.CONFLICT, "E302", "이미 좋아요한 상품입니다."),
    LIKE_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "E303", "좋아요 대상 상품을 찾을 수 없습니다."),
    LIKE_NOT_OWNER(HttpStatus.FORBIDDEN, "E304", "본인의 좋아요만 취소할 수 있습니다."),

    /** 주문 도메인 에러 */
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "E401", "주문을 찾을 수 없습니다."),
    ORDER_ALREADY_CANCELLED(HttpStatus.CONFLICT, "E402", "이미 취소된 주문입니다."),
    ORDER_ITEM_EMPTY(HttpStatus.BAD_REQUEST, "E403", "주문 상품이 비어있습니다."),
    INVALID_ORDER_QUANTITY(HttpStatus.BAD_REQUEST, "E404", "주문 수량이 올바르지 않습니다."),
    ORDER_NOT_OWNER(HttpStatus.FORBIDDEN, "E405", "본인의 주문만 조회/취소할 수 있습니다."),
    ORDER_PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "E406", "주문 대상 상품을 찾을 수 없습니다."),
}
