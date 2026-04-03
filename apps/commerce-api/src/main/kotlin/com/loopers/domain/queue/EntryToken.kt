package com.loopers.domain.queue

data class EntryToken(
    val token: String,
    val userId: Long,
    val remainingSeconds: Long = 300L,
)
