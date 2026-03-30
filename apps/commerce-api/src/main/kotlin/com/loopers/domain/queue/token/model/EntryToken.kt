package com.loopers.domain.queue.token.model

import java.util.UUID

data class EntryToken(
    val userId: Long,
    val token: String,
) {
    companion object {
        private const val TOKEN_TTL_SECONDS = 300L

        fun issue(userId: Long): EntryToken {
            return EntryToken(
                userId = userId,
                token = UUID.randomUUID().toString(),
            )
        }

        fun defaultTtlSeconds(): Long = TOKEN_TTL_SECONDS
    }
}
