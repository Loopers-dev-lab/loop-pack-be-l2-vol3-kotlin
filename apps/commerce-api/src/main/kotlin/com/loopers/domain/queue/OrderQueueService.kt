package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class OrderQueueService(
    private val orderQueueRepository: OrderQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
) {

    fun enterQueue(userId: Long): Boolean {
        val score = System.currentTimeMillis().toDouble()
        return orderQueueRepository.enqueue(userId, score)
    }

    fun getPosition(userId: Long): QueuePosition {
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

    fun admitUsers(batchSize: Long): Long {
        val userIds = orderQueueRepository.popFront(batchSize)
        userIds.forEach { userId ->
            val token = UUID.randomUUID().toString()
            entryTokenRepository.issue(userId, token, ENTRY_TOKEN_TTL_SECONDS)
        }
        return userIds.size.toLong()
    }

    fun validateAndConsumeToken(userId: Long, token: String) {
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
