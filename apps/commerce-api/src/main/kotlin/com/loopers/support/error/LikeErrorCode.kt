package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class LikeErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "LIKE_001", "좋아요를 찾을 수 없습니다."),
    ALREADY_LIKED(HttpStatus.CONFLICT, "LIKE_002", "이미 좋아요한 상품입니다."),
}
