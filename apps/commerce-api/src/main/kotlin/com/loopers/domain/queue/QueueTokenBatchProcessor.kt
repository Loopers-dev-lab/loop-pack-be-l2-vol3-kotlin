package com.loopers.domain.queue

import com.loopers.domain.queue.token.model.EntryToken

interface QueueTokenBatchProcessor {
    fun popAndIssueTokens(count: Int, ttlSeconds: Long): List<EntryToken>
}
