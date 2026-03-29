package com.loopers.domain.outbox.model

enum class OrderOutboxEventType {
    PAYMENT_COMPLETED,
    PAYMENT_FAILED,
}
