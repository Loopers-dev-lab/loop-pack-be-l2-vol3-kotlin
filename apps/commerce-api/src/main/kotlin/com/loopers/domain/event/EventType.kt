package com.loopers.domain.event

/**
 * 이벤트 타입 정의.
 *
 * @param topic Kafka 토픽명 (Step 2에서 사용)
 * @param retryInterval 재시도 간격 (밀리초)
 * @param maxRetry 최대 재시도 횟수
 */
enum class EventType(
    val topic: String,
    val retryInterval: Long = 3000L,
    val maxRetry: Int = 3,
) {
    // ── 주문 이벤트 ──
    ORDER_CREATED("order-events"),

    // ── 상품/좋아요 이벤트 ──
    PRODUCT_LIKED("catalog-events"),
    PRODUCT_UNLIKED("catalog-events"),
    PRODUCT_VIEWED("catalog-events"),

    // ── 쿠폰 이벤트 ──
    COUPON_ISSUE_REQUESTED("coupon-issue-requests"),

    // ── 유저 행동 로깅 (Kafka 발행 불필요, 내부 처리만) ──
    USER_ACTION("", retryInterval = 0, maxRetry = 0),
}
