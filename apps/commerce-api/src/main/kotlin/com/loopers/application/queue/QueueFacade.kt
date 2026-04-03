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
        private const val EVENT_ADMITTED = "admitted"
        private const val EVENT_POSITION = "position"
        private const val EVENT_BYPASS = "bypass"
    }

    fun subscribe(userId: Long): SseEmitter {
        orderQueueService.getPosition(userId)

        val emitter = SseEmitter(SSE_TIMEOUT)
        val cleanup = Runnable { queueEmitterRepository.remove(userId) }
        emitter.onCompletion(cleanup)
        emitter.onTimeout(cleanup)
        emitter.onError { cleanup.run() }
        queueEmitterRepository.add(userId, emitter)
        return emitter
    }

    fun enterQueue(userId: Long): QueuePositionInfo {
        orderQueueService.enterQueue(userId)
        val queuePosition = orderQueueService.getPosition(userId)
        return toInfo(queuePosition)
    }

    fun broadcastBypass() {
        forEachEmitter { _, emitter ->
            emitter.send(SseEmitter.event().name(EVENT_BYPASS).data("대기열이 비활성화되었습니다"))
            emitter.complete()
        }
    }

    fun broadcastPositions(admittedUserIds: List<Long>) {
        val admittedSet = admittedUserIds.toHashSet()
        val emitters = queueEmitterRepository.getAll()
        val waitingUserIds = emitters.keys.filter { it !in admittedSet }
        val positions = orderQueueService.getWaitingPositions(waitingUserIds)

        forEachEmitter(emitters) { userId, emitter ->
            if (userId in admittedSet) {
                emitter.send(SseEmitter.event().name(EVENT_ADMITTED).data("토큰이 발급되었습니다"))
                emitter.complete()
            } else {
                val position = positions[userId] ?: return@forEachEmitter
                emitter.send(SseEmitter.event().name(EVENT_POSITION).data(position))
            }
        }
    }

    private fun forEachEmitter(
        emitters: Map<Long, SseEmitter> = queueEmitterRepository.getAll(),
        block: (userId: Long, emitter: SseEmitter) -> Unit,
    ) {
        for ((userId, emitter) in emitters) {
            try {
                block(userId, emitter)
            } catch (e: Exception) {
                queueEmitterRepository.remove(userId)
            }
        }
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
