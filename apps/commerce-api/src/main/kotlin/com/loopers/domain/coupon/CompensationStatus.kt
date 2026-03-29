package com.loopers.domain.coupon

enum class CompensationStatus {
    /** 보상 대기 — 스케줄러가 재시도할 대상 */
    PENDING,
    /** 보상 완료 — 쿠폰 USED 마킹 성공 */
    RESOLVED,
    /** 보상 실패 — 재시도 소진, 수동 처리 필요 */
    FAILED,
}
