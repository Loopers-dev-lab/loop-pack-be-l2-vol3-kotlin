package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class ErrorType(val status: HttpStatus, val code: String, val message: String) {
    /** 범용 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase, "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.reasonPhrase, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.reasonPhrase, "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.reasonPhrase, "이미 존재하는 리소스입니다."),

    /** User 도메인 */
    USER_DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "USER_DUPLICATE_LOGIN_ID", "이미 사용 중인 로그인 ID입니다."),
    USER_INVALID_LOGIN_ID(HttpStatus.BAD_REQUEST, "USER_INVALID_LOGIN_ID", "로그인 ID는 영문 대소문자와 숫자만 사용할 수 있습니다."),
    USER_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USER_INVALID_PASSWORD", "비밀번호는 영문, 숫자, 허용된 특수문자만 사용할 수 있습니다."),
    USER_INVALID_NAME(HttpStatus.BAD_REQUEST, "USER_INVALID_NAME", "이름은 한글만 입력할 수 있습니다."),
}
