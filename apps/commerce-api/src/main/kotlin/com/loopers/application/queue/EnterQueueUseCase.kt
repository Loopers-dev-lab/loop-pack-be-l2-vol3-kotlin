package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueStore
import com.loopers.support.error.CoreException
import com.loopers.support.error.QueueErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.stereotype.Component

@Component
class EnterQueueUseCase(
    private val orderQueueStore: OrderQueueStore,
    private val queueProperties: QueueProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(userId: Long): QueueEntryInfo {
        try {
            return doExecute(userId)
        } catch (e: CoreException) {
            throw e
        } catch (e: RedisConnectionFailureException) {
            log.error("대기열 진입 실패 — Redis 연결 오류 [userId={}]", userId, e)
            throw CoreException(QueueErrorCode.QUEUE_SERVICE_UNAVAILABLE)
        } catch (e: Exception) {
            log.error("대기열 진입 실패 — 예상치 못한 오류 [userId={}]", userId, e)
            throw CoreException(QueueErrorCode.QUEUE_SERVICE_UNAVAILABLE)
        }
    }

    private fun doExecute(userId: Long): QueueEntryInfo {
        if (orderQueueStore.hasToken(userId)) {
            return QueueEntryInfo(
                position = 0,
                estimatedWaitSeconds = 0,
                alreadyHasToken = true,
            )
        }

        val result = orderQueueStore.enqueue(userId, queueProperties.maxQueueSize)

        if (result == ENQUEUE_ALREADY_IN_QUEUE) {
            val position = orderQueueStore.getPosition(userId) ?: 0L
            return QueueEntryInfo(
                position = position,
                estimatedWaitSeconds = QueueCalculator.estimatedWaitSeconds(
                    position, queueProperties.scheduler.batchSize, queueProperties.scheduler.intervalMs,
                ),
                alreadyHasToken = false,
            )
        }

        if (result == ENQUEUE_QUEUE_FULL) {
            throw CoreException(QueueErrorCode.QUEUE_FULL)
        }

        val position = result
        val estimatedWaitSeconds = QueueCalculator.estimatedWaitSeconds(
            position, queueProperties.scheduler.batchSize, queueProperties.scheduler.intervalMs,
        )

        return QueueEntryInfo(
            position = position,
            estimatedWaitSeconds = estimatedWaitSeconds,
            alreadyHasToken = false,
        )
    }

    companion object {
        private const val ENQUEUE_ALREADY_IN_QUEUE = -1L
        private const val ENQUEUE_QUEUE_FULL = -2L
    }
}
