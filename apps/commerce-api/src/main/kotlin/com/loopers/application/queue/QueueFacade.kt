package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueService
import com.loopers.domain.queue.QueueEmitterRepository
import com.loopers.domain.queue.QueuePosition
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Component
class QueueFacade(
    private val orderQueueService: OrderQueueService,
    private val queueEmitterRepository: QueueEmitterRepository,
) {

    companion object {
        private const val SSE_TIMEOUT = 300_000L // 5분
    }

    fun subscribe(userId: Long): SseEmitter {
        val emitter = SseEmitter(SSE_TIMEOUT)
        queueEmitterRepository.add(userId, emitter)
        return emitter
    }

    fun enterQueue(userId: Long): QueuePositionInfo {
        orderQueueService.enterQueue(userId)
        val queuePosition = orderQueueService.getPosition(userId)
        return toInfo(queuePosition)
    }

    fun getPosition(userId: Long): QueuePositionInfo {
        val queuePosition = orderQueueService.getPosition(userId)
        return toInfo(queuePosition)
    }

    private fun toInfo(queuePosition: QueuePosition): QueuePositionInfo {
        val pollingIntervalMs = if (queuePosition.token != null) 0L else calculatePollingInterval(queuePosition.position)
        return QueuePositionInfo.from(queuePosition, pollingIntervalMs)
    }

    private fun calculatePollingInterval(position: Long): Long {
        return when {
            position <= 10 -> 1000L
            position <= 50 -> 2000L
            position <= 200 -> 3000L
            else -> 5000L
        }
    }
}
