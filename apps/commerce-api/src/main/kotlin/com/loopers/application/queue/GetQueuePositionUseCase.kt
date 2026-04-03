package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueStore
import com.loopers.support.error.CoreException
import com.loopers.support.error.QueueErrorCode
import org.slf4j.LoggerFactory
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.stereotype.Component

@Component
class GetQueuePositionUseCase(
    private val orderQueueStore: OrderQueueStore,
    private val queueProperties: QueueProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(userId: Long): QueuePositionInfo {
        try {
            return doExecute(userId)
        } catch (e: CoreException) {
            throw e
        } catch (e: RedisConnectionFailureException) {
            log.error("순번 조회 실패 — Redis 연결 오류 [userId={}]", userId, e)
            throw CoreException(QueueErrorCode.QUEUE_SERVICE_UNAVAILABLE)
        } catch (e: Exception) {
            log.error("순번 조회 실패 — 예상치 못한 오류 [userId={}]", userId, e)
            throw CoreException(QueueErrorCode.QUEUE_SERVICE_UNAVAILABLE)
        }
    }

    private fun doExecute(userId: Long): QueuePositionInfo {
        if (orderQueueStore.hasToken(userId)) {
            return QueuePositionInfo.ready()
        }

        val position = orderQueueStore.getPosition(userId)
            ?: return QueuePositionInfo.notInQueue()

        val estimatedWaitSeconds = QueueCalculator.estimatedWaitSeconds(
            position, queueProperties.scheduler.batchSize, queueProperties.scheduler.intervalMs,
        )
        val suggestedPollIntervalMs = QueueCalculator.suggestedPollIntervalMs(position)

        return QueuePositionInfo.waiting(
            position = position,
            estimatedWaitSeconds = estimatedWaitSeconds,
            suggestedPollIntervalMs = suggestedPollIntervalMs,
        )
    }
}
