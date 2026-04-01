package com.loopers.application.queue

import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class IssueEntryTokensUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val queueProperties: QueueProperties,
) {

    fun execute(): List<IssuedTokenInfo> {
        val results = waitingQueueRepository.popMinAndIssueTokens(
            count = queueProperties.batchSize,
            ttlSeconds = queueProperties.tokenTtlSeconds,
        )
        if (results.isEmpty()) return emptyList()

        applyJitter()

        return results.map { (userId, token) ->
            IssuedTokenInfo(userId = userId.value, token = token)
        }
    }

    private fun applyJitter() {
        val maxMs = queueProperties.jitterMaxMs
        if (maxMs <= 0) return
        Thread.sleep(Random.nextLong(0, maxMs + 1))
    }
}
