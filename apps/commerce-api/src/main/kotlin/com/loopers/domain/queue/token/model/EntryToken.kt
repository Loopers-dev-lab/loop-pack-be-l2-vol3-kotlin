package com.loopers.domain.queue.token.model

import com.loopers.domain.common.vo.UserId
import java.util.UUID

data class EntryToken(
    val userId: UserId,
    val token: String,
) {
    companion object {
        private const val DEFAULT_TOKEN_TTL_SECONDS = 300L

        fun issue(userId: UserId): EntryToken {
            return EntryToken(
                userId = userId,
                token = UUID.randomUUID().toString(),
            )
        }

        fun defaultTtlSeconds(): Long = DEFAULT_TOKEN_TTL_SECONDS
    }
}
