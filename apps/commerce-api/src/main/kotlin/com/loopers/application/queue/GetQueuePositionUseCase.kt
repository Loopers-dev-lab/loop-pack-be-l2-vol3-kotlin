package com.loopers.application.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.WaitingQueueRepository
import org.springframework.stereotype.Component

@Component
class GetQueuePositionUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
) {
    companion object {
        private const val SECONDS_PER_USER = 0.006
    }

    fun getPosition(userId: Long): QueuePositionResult {
        val token = entryTokenRepository.findToken(userId)
        if (token != null) {
            return QueuePositionResult.authorized(token)
        }

        val rank = waitingQueueRepository.getPosition(userId)
            ?: return QueuePositionResult.notInQueue()

        val position = rank + 1
        val estimatedWaitSeconds = (position * SECONDS_PER_USER).toLong().coerceAtLeast(1)

        return QueuePositionResult.waiting(position, estimatedWaitSeconds)
    }
}
