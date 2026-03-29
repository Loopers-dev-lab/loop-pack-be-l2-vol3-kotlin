package com.loopers.domain.outbox

enum class OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED,
}
