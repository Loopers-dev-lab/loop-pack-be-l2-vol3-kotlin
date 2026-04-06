package com.loopers.application.queue

import com.loopers.domain.queue.QueueService
import org.springframework.stereotype.Component

@Component
class QueueFacade(
    private val queueService: QueueService,
) {

    fun enterQueue(criteria: EnterQueueCriteria): QueueEntryResult {
        val info = queueService.enterQueue(criteria.userId)
        return QueueEntryResult.from(info)
    }

    fun getPosition(criteria: GetPositionCriteria): QueuePositionResult {
        val info = queueService.getPosition(criteria.userId)
        return QueuePositionResult.from(info)
    }

    fun enableQueue(): QueueStatusResult {
        queueService.enableQueue()
        return QueueStatusResult(enabled = true)
    }

    fun disableQueue(): QueueStatusResult {
        queueService.disableQueue()
        return QueueStatusResult(enabled = false)
    }

    fun getQueueStatus(): QueueStatusResult {
        return QueueStatusResult(enabled = queueService.isQueueEnabled())
    }
}
