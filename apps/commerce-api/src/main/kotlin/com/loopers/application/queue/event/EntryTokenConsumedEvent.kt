package com.loopers.application.queue.event

data class EntryTokenConsumedEvent(
    val userId: Long,
    val orderId: Long,
)
