package com.loopers.domain.outbox

enum class OutboxEventType(val topic: String) {
    LIKE_CREATED("catalog-events"),
    LIKE_CANCELLED("catalog-events"),
    ORDER_CREATED("order-events"),
    PAYMENT_APPROVED("order-events"),
    PAYMENT_FAILED("order-events"),
    COUPON_ISSUE_REQUESTED("coupon-issue-requests"),
}
