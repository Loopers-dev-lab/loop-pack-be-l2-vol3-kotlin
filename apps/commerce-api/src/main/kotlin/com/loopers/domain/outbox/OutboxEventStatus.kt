package com.loopers.domain.outbox

enum class OutboxEventStatus {
    PENDING,
    SENDING,
    PUBLISHED,
    FAILED,
    DEAD,
}
