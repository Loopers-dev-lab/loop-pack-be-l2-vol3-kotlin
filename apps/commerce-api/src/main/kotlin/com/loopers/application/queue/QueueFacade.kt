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

        // emitter 등록 전에 스케줄러가 토큰을 발급했을 수 있으므로 현재 상태를 재조회
        val currentPosition = orderQueueService.getPosition(userId)
        if (currentPosition.token != null) {
            emitter.send(SseEmitter.event().name(EVENT_ADMITTED).data(mapOf("token" to currentPosition.token)))
            emitter.complete()
        }

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

    fun broadcastPositions(admittedUsers: Map<Long, String>) {
        val emitters = queueEmitterRepository.getAll()
        val waitingUserIds = emitters.keys.filter { it !in admittedUsers }
        val positions = orderQueueService.getWaitingPositions(waitingUserIds)

        forEachEmitter(emitters) { userId, emitter ->
            val token = admittedUsers[userId]
            if (token != null) {
                emitter.send(SseEmitter.event().name(EVENT_ADMITTED).data(mapOf("token" to token)))
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
        val pollingIntervalMs = if (queuePosition.token != null || queuePosition.bypassed) {
            0L
        } else {
            calculatePollingInterval(queuePosition.position)
        }
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
