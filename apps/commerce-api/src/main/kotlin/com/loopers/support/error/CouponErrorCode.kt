package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class CouponErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_001", "쿠폰을 찾을 수 없습니다."),
    COUPON_EXPIRED(HttpStatus.BAD_REQUEST, "COUPON_002", "만료된 쿠폰입니다."),
    COUPON_ALREADY_USED(HttpStatus.BAD_REQUEST, "COUPON_003", "이미 사용된 쿠폰입니다."),
    COUPON_NOT_OWNED(HttpStatus.FORBIDDEN, "COUPON_004", "본인의 쿠폰이 아닙니다."),
    USER_COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "COUPON_005", "발급된 쿠폰을 찾을 수 없습니다."),
    ALREADY_ISSUED_COUPON(HttpStatus.CONFLICT, "COUPON_006", "이미 발급받은 쿠폰입니다."),
    INVALID_COUPON_VALUE(HttpStatus.BAD_REQUEST, "COUPON_007", "할인 값이 유효하지 않습니다."),
    INVALID_COUPON_NAME(HttpStatus.BAD_REQUEST, "COUPON_008", "쿠폰명은 1~50자여야 합니다."),
    INVALID_MIN_ORDER_AMOUNT(HttpStatus.BAD_REQUEST, "COUPON_009", "최소 주문 금액은 0 이상이어야 합니다."),
    MIN_ORDER_AMOUNT_NOT_MET(HttpStatus.BAD_REQUEST, "COUPON_010", "최소 주문 금액 조건을 충족하지 않습니다."),
    INVALID_RATE_VALUE(HttpStatus.BAD_REQUEST, "COUPON_011", "정률 할인은 1~100 사이여야 합니다."),
}
