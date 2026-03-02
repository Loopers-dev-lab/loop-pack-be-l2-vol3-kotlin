package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class BrandErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "BRAND_001", "브랜드를 찾을 수 없습니다."),
    DUPLICATE_BRAND_NAME(HttpStatus.CONFLICT, "BRAND_002", "이미 사용 중인 브랜드명입니다."),
    INVALID_BRAND_NAME(HttpStatus.BAD_REQUEST, "BRAND_003", "브랜드명은 1~50자여야 합니다."),
}
