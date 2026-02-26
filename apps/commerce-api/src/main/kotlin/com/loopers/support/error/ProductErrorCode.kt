package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class ProductErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_001", "상품을 찾을 수 없습니다."),
    INVALID_PRODUCT_NAME(HttpStatus.BAD_REQUEST, "PRODUCT_002", "상품명은 1~100자여야 합니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT_003", "재고가 부족합니다."),
    INVALID_BRAND(HttpStatus.BAD_REQUEST, "PRODUCT_004", "유효하지 않은 브랜드입니다."),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, "PRODUCT_005", "가격은 0 이상이어야 합니다."),
    INVALID_STOCK(HttpStatus.BAD_REQUEST, "PRODUCT_006", "재고는 0 이상이어야 합니다."),
}
