package com.loopers.domain.outbox

enum class OutboxEventStatus {
    PENDING,
    PUBLISHED,
    FAILED,
}
