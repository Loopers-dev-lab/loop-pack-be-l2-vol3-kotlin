package com.loopers.domain.queue.token.model

/**
 * 토큰 원자적 소비 결과.
 */
enum class EntryTokenConsumeResult {
    /** 토큰 일치 + 삭제 완료 */
    SUCCESS,

    /** 토큰 없음 또는 만료 */
    NOT_FOUND,

    /** 저장된 토큰과 불일치 */
    MISMATCH,
}
