package com.loopers.application.queue

import com.loopers.domain.queue.QueueService
import com.loopers.domain.queue.dto.QueueEntryInfo
import com.loopers.domain.queue.dto.QueuePositionInfo
import com.loopers.domain.queue.dto.QueueStatusInfo
import org.springframework.stereotype.Component

@Component
class QueueFacade(
    private val queueService: QueueService,
) {

    fun enter(
        queueName: String,
        userId: Long,
        throughputPerServerPerSecond: Int,
    ): QueueEntryInfo = queueService.enter(queueName, userId, throughputPerServerPerSecond)

    fun getPosition(
        queueName: String,
        userId: Long,
        throughputPerServerPerSecond: Int,
    ): QueuePositionInfo = queueService.getPosition(queueName, userId, throughputPerServerPerSecond)

    fun getStatus(
        queueName: String,
        throughputPerServerPerSecond: Int,
    ): QueueStatusInfo = queueService.getStatus(queueName, throughputPerServerPerSecond)
}
