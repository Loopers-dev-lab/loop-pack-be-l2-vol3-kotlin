package com.loopers.domain.queue

enum class QueueErrorType(val code: String, val message: String) {
    QUEUE_DISABLED("Queue Disabled", "대기열이 비활성 상태입니다."),
    ALREADY_IN_QUEUE("Already In Queue", "이미 대기열에 진입한 상태입니다."),
    NOT_IN_QUEUE("Not In Queue", "대기열에 존재하지 않습니다."),
    INVALID_TOKEN("Invalid Token", "유효한 대기열 토큰이 필요합니다."),
    TOKEN_EXPIRED("Token Expired", "대기열 토큰이 만료되었습니다."),
}
