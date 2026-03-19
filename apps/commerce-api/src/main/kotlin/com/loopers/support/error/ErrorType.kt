package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class ErrorType(val status: HttpStatus, val code: String, val message: String) {
    /** 범용 에러 */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase, "일시적인 오류가 발생했습니다."),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, HttpStatus.BAD_REQUEST.reasonPhrase, "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.reasonPhrase, "인증에 실패했습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN.reasonPhrase, "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.reasonPhrase, "존재하지 않는 요청입니다."),
    CONFLICT(HttpStatus.CONFLICT, HttpStatus.CONFLICT.reasonPhrase, "이미 존재하는 리소스입니다."),

    /** PG 연동 에러 */
    PG_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "PG_RATE_LIMITED", "결제 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    PG_CIRCUIT_OPEN(HttpStatus.SERVICE_UNAVAILABLE, "PG_CIRCUIT_OPEN", "결제 시스템이 일시적으로 중단되었습니다. 잠시 후 다시 시도해주세요."),
    PG_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "PG_TIMEOUT", "결제 시스템 응답 지연이 발생했습니다. 결제 상태를 확인 중입니다."),
    PG_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "PG_SERVER_ERROR", "결제 시스템에 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
}
