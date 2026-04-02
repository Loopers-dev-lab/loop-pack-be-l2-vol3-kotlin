package com.loopers.application.queue

import com.loopers.infrastructure.queue.WaitingQueueRedisRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class QueueService(
    private val waitingQueueRedisRepository: WaitingQueueRedisRepository,
) {

    private val tokenIssuedCount = AtomicLong(0)
    private val tokenConsumedCount = AtomicLong(0)

    fun enterQueue(userId: Long): QueueEntryInfo {
        if (waitingQueueRedisRepository.hasToken(userId)) {
            return QueueEntryInfo(
                position = 0,
                estimatedWaitSeconds = 0,
                totalWaiting = waitingQueueRedisRepository.getTotalCount(),
            )
        }

        val position = waitingQueueRedisRepository.addToQueue(userId)
        val totalWaiting = waitingQueueRedisRepository.getTotalCount()

        return QueueEntryInfo(
            position = position,
            estimatedWaitSeconds = calculateEstimatedWaitSeconds(position),
            totalWaiting = totalWaiting,
        )
    }

    fun getQueuePosition(userId: Long): QueuePositionInfo {
        val token = waitingQueueRedisRepository.getToken(userId)
        if (token != null) {
            return QueuePositionInfo(
                position = 0,
                estimatedWaitSeconds = 0,
                totalWaiting = waitingQueueRedisRepository.getTotalCount(),
                token = token,
            )
        }

        val position = waitingQueueRedisRepository.getPosition(userId)
            ?: return QueuePositionInfo(
                position = 0,
                estimatedWaitSeconds = 0,
                totalWaiting = waitingQueueRedisRepository.getTotalCount(),
                token = null,
            )

        return QueuePositionInfo(
            position = position,
            estimatedWaitSeconds = calculateEstimatedWaitSeconds(position),
            totalWaiting = waitingQueueRedisRepository.getTotalCount(),
            token = null,
        )
    }

    fun issueTokens(batchSize: Long): Long {
        val issuedUserIds = waitingQueueRedisRepository.popAndIssueTokens(batchSize)
        tokenIssuedCount.addAndGet(issuedUserIds.size.toLong())
        return issuedUserIds.size.toLong()
    }

    fun validateAndConsumeToken(userId: Long) {
        val token = waitingQueueRedisRepository.getToken(userId)
            ?: throw CoreException(ErrorType.FORBIDDEN, "입장 토큰이 필요합니다.")
    }

    fun incrementConsumedCount() {
        tokenConsumedCount.incrementAndGet()
    }

    fun getTokenIssuedCount(): Long = tokenIssuedCount.get()

    fun getTokenConsumedCount(): Long = tokenConsumedCount.get()

    fun getTokenExpiryRate(): Double {
        val issued = tokenIssuedCount.get()
        if (issued == 0L) return 0.0
        val consumed = tokenConsumedCount.get()
        return (issued - consumed).toDouble() / issued.toDouble()
    }

    private fun calculateEstimatedWaitSeconds(position: Long): Long {
        if (position <= 0) return 0
        return position / THROUGHPUT_PER_SECOND
    }

    companion object {
        private const val THROUGHPUT_PER_SECOND = 175L
    }
}
