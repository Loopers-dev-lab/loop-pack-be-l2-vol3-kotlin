package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    INVALID_PASSWORD_LENGTH(HttpStatus.BAD_REQUEST, "USER_001", "비밀번호는 8~16자여야 합니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "USER_002", "비밀번호는 영문 대소문자, 숫자, 특수문자만 사용 가능합니다."),
    PASSWORD_CONTAINS_BIRTH_DATE(HttpStatus.BAD_REQUEST, "USER_003", "비밀번호에 생년월일을 포함할 수 없습니다."),
}
