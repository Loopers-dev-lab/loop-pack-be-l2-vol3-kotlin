package com.loopers.domain.orderqueue

import com.loopers.infrastructure.orderqueue.OrderQueueProperties
import com.loopers.infrastructure.orderqueue.OrderQueueRedisRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service

@Service
class OrderQueueService(
    private val orderQueueRedisRepository: OrderQueueRedisRepository,
    private val orderQueueProperties: OrderQueueProperties,
) {

    fun enter(userId: Long): QueueEntryInfo {
        val added = orderQueueRedisRepository.enqueue(userId)
        if (added == 0L) {
            throw CoreException(ErrorType.CONFLICT, "이미 대기열에 진입한 상태입니다.")
        }

        val position = orderQueueRedisRepository.getPosition(userId)
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "대기열 진입 후 순번 조회에 실패했습니다.")
        val totalWaiting = orderQueueRedisRepository.getTotalSize()

        return QueueEntryInfo(
            position = position,
            totalWaiting = totalWaiting,
            estimatedWaitSeconds = calculateEstimatedWaitSeconds(position),
            pollingIntervalSeconds = calculatePollingInterval(position),
        )
    }

    fun getPosition(userId: Long): QueuePositionInfo {
        val ttl = orderQueueRedisRepository.getTokenTtl(userId)
        if (ttl > 0) {
            return QueuePositionInfo(
                status = QueueStatus.ACTIVE,
                tokenExpireSeconds = ttl,
            )
        }

        val position = orderQueueRedisRepository.getPosition(userId)
            ?: return QueuePositionInfo(status = QueueStatus.NOT_IN_QUEUE)

        val totalWaiting = orderQueueRedisRepository.getTotalSize()

        return QueuePositionInfo(
            status = QueueStatus.WAITING,
            position = position,
            totalWaiting = totalWaiting,
            estimatedWaitSeconds = calculateEstimatedWaitSeconds(position),
            pollingIntervalSeconds = calculatePollingInterval(position),
        )
    }

    fun validateToken(userId: Long) {
        if (!orderQueueRedisRepository.hasToken(userId)) {
            throw CoreException(ErrorType.BAD_REQUEST, "입장 토큰이 없습니다. 대기열에 진입해주세요.")
        }
    }

    fun consumeToken(userId: Long) {
        orderQueueRedisRepository.consumeToken(userId)
    }

    fun processTokenIssuance(batchSize: Long) {
        orderQueueRedisRepository.dequeueAndIssueTokens(batchSize, orderQueueProperties.tokenTtlSeconds)
    }

    fun calculateEstimatedWaitSeconds(position: Long): Long {
        return (position + orderQueueProperties.throughputPerSecond - 1) / orderQueueProperties.throughputPerSecond
    }

    fun calculatePollingInterval(position: Long): Int {
        return when {
            position <= 100 -> 2
            position <= 1_000 -> 5
            position <= 5_000 -> 10
            else -> 30
        }
    }
}
