package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class OrderErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    ORDER_VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "ORDER_001", "주문 검증에 실패했습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_002", "주문을 찾을 수 없습니다."),
    ORDER_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ORDER_003", "해당 주문에 접근할 수 없습니다."),
    EMPTY_ORDER_ITEMS(HttpStatus.BAD_REQUEST, "ORDER_004", "주문 항목이 비어있습니다."),
    DUPLICATE_ORDER_ITEM(HttpStatus.BAD_REQUEST, "ORDER_005", "동일한 상품이 중복되었습니다."),
    EXCEED_MAX_ORDER_TYPES(HttpStatus.BAD_REQUEST, "ORDER_006", "주문 상품 종류는 최대 20종까지 가능합니다."),
    EXCEED_MAX_ORDER_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER_007", "상품당 최대 주문 수량은 99개입니다."),
    INVALID_ORDER_PERIOD(HttpStatus.BAD_REQUEST, "ORDER_008", "조회 기간이 올바르지 않습니다. 기간을 지정해주세요. (최대 365일)"),
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER_009", "주문 수량은 1 이상이어야 합니다."),
}
