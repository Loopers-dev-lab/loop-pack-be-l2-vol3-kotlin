package com.loopers.application.queue

import com.loopers.domain.queue.WaitingQueueService
import org.springframework.stereotype.Component

@Component
class QueueFacade(
    private val waitingQueueService: WaitingQueueService,
) {
    fun enterQueue(userId: Long): QueueEntryInfo {
        val result = waitingQueueService.enterQueue(userId)
        return QueueEntryInfo(
            position = result.position,
            totalWaiting = result.totalWaiting,
            estimatedWaitSeconds = result.estimatedWaitSeconds,
        )
    }

    fun getQueuePosition(userId: Long): QueuePositionInfo {
        val result = waitingQueueService.getQueuePosition(userId)
        return QueuePositionInfo(
            position = result.position,
            totalWaiting = result.totalWaiting,
            estimatedWaitSeconds = result.estimatedWaitSeconds,
            token = result.token,
        )
    }

    fun validateEntryToken(userId: Long): String {
        return waitingQueueService.validateEntryToken(userId)
    }

    fun consumeEntryToken(userId: Long) {
        waitingQueueService.consumeEntryToken(userId)
    }
}
