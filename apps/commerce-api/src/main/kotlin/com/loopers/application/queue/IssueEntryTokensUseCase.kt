package com.loopers.application.queue

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
        val poppedUsers = waitingQueueRepository.popMin(queueProperties.batchSize)
        if (poppedUsers.isEmpty()) return emptyList()

        val perUserDelayMs = calculatePerUserDelayMs(poppedUsers.size)

        return poppedUsers.mapIndexed { index, userId ->
            val entryToken = EntryToken.issue(userId)
            entryTokenRepository.issue(userId, entryToken.token, queueProperties.tokenTtlSeconds)

            if (perUserDelayMs > 0 && index < poppedUsers.size - 1) {
                Thread.sleep(Random.nextLong(0, perUserDelayMs + 1))
            }

            IssuedTokenInfo(userId = userId.value, token = entryToken.token)
        }
    }

    private fun calculatePerUserDelayMs(userCount: Int): Long {
        val maxMs = queueProperties.jitterMaxMs
        if (maxMs <= 0 || userCount <= 1) return 0
        return maxMs / userCount
    }
}
