package com.loopers.application.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.model.EntryToken
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class IssueEntryTokensUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
    private val queueProperties: QueueProperties,
) {

    fun execute(): List<IssuedTokenInfo> {
        val userIds = waitingQueueRepository.popMin(queueProperties.batchSize)
        if (userIds.isEmpty()) return emptyList()

        applyJitter()

        return userIds.map { userId ->
            val entryToken = EntryToken.issue(UserId(userId))
            entryTokenRepository.issue(
                userId = entryToken.userId,
                token = entryToken.token,
                ttlSeconds = queueProperties.tokenTtlSeconds,
            )
            IssuedTokenInfo(userId = userId, token = entryToken.token)
        }
    }

    private fun applyJitter() {
        val maxMs = queueProperties.jitterMaxMs
        if (maxMs <= 0) return
        Thread.sleep(Random.nextLong(0, maxMs + 1))
    }
}
