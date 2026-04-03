package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrderQueueService(
    private val orderQueueRepository: OrderQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
    private val queueHealthChecker: QueueHealthChecker,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun enterQueue(userId: Long): Boolean {
        val score = System.currentTimeMillis().toDouble()
        return orderQueueRepository.enqueue(userId, score)
    }

    fun getPosition(userId: Long): QueuePosition {
        if (queueHealthChecker.isBypassed()) {
            log.warn("[BYPASS_AUDIT] 대기열 bypass 모드: 순번 조회 스킵. userId={}", userId)
            return QueuePosition(
                position = 0L,
                estimatedWaitSeconds = 0.0,
                totalSize = 0L,
                bypassed = true,
            )
        }

        val token = entryTokenRepository.get(userId)
        if (token != null) {
            return QueuePosition(
                position = 0L,
                estimatedWaitSeconds = 0.0,
                totalSize = 0L,
                token = token,
            )
        }

        val position = orderQueueRepository.getPosition(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "대기열에 존재하지 않는 유저입니다. userId=$userId")

        val totalSize = orderQueueRepository.getTotalSize()
        val estimatedWaitSeconds = position / ADMIT_RATE_PER_SECOND

        return QueuePosition(
            position = position,
            estimatedWaitSeconds = estimatedWaitSeconds,
            totalSize = totalSize,
        )
    }

    fun getWaitingPositions(userIds: List<Long>): Map<Long, QueuePosition> {
        if (userIds.isEmpty()) return emptyMap()
        val totalSize = orderQueueRepository.getTotalSize()
        return userIds.mapNotNull { userId ->
            val position = orderQueueRepository.getPosition(userId) ?: return@mapNotNull null
            val estimatedWaitSeconds = position / ADMIT_RATE_PER_SECOND
            userId to QueuePosition(position = position, estimatedWaitSeconds = estimatedWaitSeconds, totalSize = totalSize)
        }.toMap()
    }

    fun admitUsers(batchSize: Long): List<Long> {
        val userIds = orderQueueRepository.popFront(batchSize)
        userIds.forEach { userId ->
            val token = UUID.randomUUID().toString()
            entryTokenRepository.issue(userId, token, ENTRY_TOKEN_TTL_SECONDS)
        }
        return userIds
    }

    fun validateAndConsumeToken(userId: Long, token: String) {
        if (queueHealthChecker.isBypassed()) {
            log.warn("[BYPASS_AUDIT] 대기열 bypass 모드: 토큰 검증 스킵. userId={}, requestToken={}", userId, token)
            return
        }

        val storedToken = entryTokenRepository.get(userId)
            ?: throw CoreException(ErrorType.FORBIDDEN, "입장 토큰이 존재하지 않습니다. userId=$userId")

        if (storedToken != token) {
            throw CoreException(ErrorType.FORBIDDEN, "입장 토큰이 일치하지 않습니다. userId=$userId")
        }

        entryTokenRepository.consume(userId)
    }

    companion object {
        private const val ENTRY_TOKEN_TTL_SECONDS = 300L
        private const val ADMIT_RATE_PER_SECOND = 175.0
    }
}
