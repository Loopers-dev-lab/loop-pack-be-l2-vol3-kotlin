package com.loopers.support.error

import org.springframework.http.HttpStatus

enum class QueueErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    QUEUE_FULL(HttpStatus.TOO_MANY_REQUESTS, "QUEUE_001", "대기열이 가득 찼습니다. 잠시 후 다시 시도해주세요."),
    QUEUE_NOT_FOUND(HttpStatus.NOT_FOUND, "QUEUE_002", "대기열 정보를 찾을 수 없습니다."),
    ENTRY_TOKEN_REQUIRED(HttpStatus.FORBIDDEN, "QUEUE_003", "대기열을 통해 입장해주세요."),
    QUEUE_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "QUEUE_004", "대기열 서비스를 일시적으로 사용할 수 없습니다."),
}
