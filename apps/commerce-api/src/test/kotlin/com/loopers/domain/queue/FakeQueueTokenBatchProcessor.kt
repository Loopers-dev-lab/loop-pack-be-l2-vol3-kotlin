package com.loopers.domain.queue

import com.loopers.domain.queue.token.model.EntryToken
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.domain.queue.waiting.FakeWaitingQueueRepository

class FakeQueueTokenBatchProcessor(
    private val waitingQueueRepository: FakeWaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
) : QueueTokenBatchProcessor {

    override fun popAndIssueTokens(count: Int, ttlSeconds: Long): List<EntryToken> {
        if (count <= 0) return emptyList()

        return waitingQueueRepository.popMin(count).map { userId ->
            val entryToken = EntryToken.issue(userId)
            entryTokenRepository.issue(userId, entryToken.token, ttlSeconds)
            entryToken
        }
    }
}
