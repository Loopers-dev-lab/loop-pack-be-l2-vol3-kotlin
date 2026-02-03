package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class CommonErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_002", "잘못된 입력값입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "COMMON_003", "잘못된 타입입니다."),
    MISSING_REQUIRED_VALUE(HttpStatus.BAD_REQUEST, "COMMON_004", "필수값이 누락되었습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_005", "요청한 리소스를 찾을 수 없습니다."),
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "COMMON_006", "이미 존재하는 리소스입니다."),
}
