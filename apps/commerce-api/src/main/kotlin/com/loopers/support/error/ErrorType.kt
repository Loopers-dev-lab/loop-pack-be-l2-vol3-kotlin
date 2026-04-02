package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class ErrorType(val status: HttpStatus, val code: String, val message: String) {
    /** 범용 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase, "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.reasonPhrase, "잘못된 요청입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.reasonPhrase, "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.reasonPhrase, "이미 존재하는 리소스입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.reasonPhrase, "인증에 실패했습니다."),

    /** 대기열 에러 */
    QUEUE_ALREADY_ENTERED(HttpStatus.CONFLICT, "QUEUE_ALREADY_ENTERED", "이미 대기열에 등록되어 있습니다."),
    QUEUE_TOKEN_REQUIRED(HttpStatus.FORBIDDEN, "QUEUE_TOKEN_REQUIRED", "주문을 위해 대기열 토큰이 필요합니다."),
    QUEUE_TOKEN_INVALID(HttpStatus.FORBIDDEN, "QUEUE_TOKEN_INVALID", "유효하지 않은 대기열 토큰입니다."),
    QUEUE_NOT_ENABLED(HttpStatus.BAD_REQUEST, "QUEUE_NOT_ENABLED", "현재 대기열이 활성화되어 있지 않습니다."),
}
