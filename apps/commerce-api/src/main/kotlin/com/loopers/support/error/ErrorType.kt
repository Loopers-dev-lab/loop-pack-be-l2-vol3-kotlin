package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class ErrorType(val status: HttpStatus, val code: String, val message: String) {
    /** 범용 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase, "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.reasonPhrase, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.reasonPhrase, "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.reasonPhrase, "이미 존재하는 리소스입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.reasonPhrase, "접근할 수 없는 리소스입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.reasonPhrase, "인증이 필요합니다."),
    SERVICE_TEMPORARILY_UNAVAILABLE(
        HttpStatus.SERVICE_UNAVAILABLE,
        "Service Temporarily Unavailable",
        "서비스가 일시적으로 이용 불가능합니다. 잠시 후 다시 시도해주세요.",
    ),

    /** 대기열 에러 */
    QUEUE_NOT_FOUND(HttpStatus.NOT_FOUND, "Queue Not Found", "존재하지 않는 대기열입니다."),
    QUEUE_USER_NOT_REGISTERED(HttpStatus.NOT_FOUND, "Queue User Not Registered", "대기열에 등록되지 않은 사용자입니다."),
    ENTRY_TOKEN_MISSING(HttpStatus.FORBIDDEN, "Entry Token Missing", "입장 토큰이 필요합니다."),
    ENTRY_TOKEN_INVALID(HttpStatus.FORBIDDEN, "Entry Token Invalid", "유효하지 않은 입장 토큰입니다."),
}
