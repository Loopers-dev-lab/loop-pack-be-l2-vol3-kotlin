package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class WaitingQueueService(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
) {
    companion object {
        const val TOKEN_TTL_SECONDS = 300L
        const val BATCH_SIZE = 18L
        const val SCHEDULER_INTERVAL_MS = 100L
        const val THROUGHPUT_PER_SECOND = 175.0
    }

    fun enterQueue(userId: Long): QueueEntryResult {
        if (entryTokenRepository.hasToken(userId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 입장 토큰이 발급된 사용자입니다.")
        }

        val score = System.currentTimeMillis().toDouble()
        val isNewEntry = waitingQueueRepository.enter(userId, score)

        if (!isNewEntry) {
            throw CoreException(ErrorType.CONFLICT, "이미 대기열에 진입한 사용자입니다.")
        }

        val position = waitingQueueRepository.getPosition(userId)
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "대기열 순번 조회에 실패했습니다.")
        val totalWaiting = waitingQueueRepository.getTotalWaitingCount()

        return QueueEntryResult(
            position = position + 1,
            totalWaiting = totalWaiting,
            estimatedWaitSeconds = calculateEstimatedWaitSeconds(position),
        )
    }

    fun getQueuePosition(userId: Long): QueuePositionResult {
        val token = entryTokenRepository.getToken(userId)
        if (token != null) {
            return QueuePositionResult(
                position = 0,
                totalWaiting = waitingQueueRepository.getTotalWaitingCount(),
                estimatedWaitSeconds = 0,
                token = token,
            )
        }

        val position = waitingQueueRepository.getPosition(userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "대기열에 존재하지 않는 사용자입니다.")

        val totalWaiting = waitingQueueRepository.getTotalWaitingCount()

        return QueuePositionResult(
            position = position + 1,
            totalWaiting = totalWaiting,
            estimatedWaitSeconds = calculateEstimatedWaitSeconds(position),
            token = null,
        )
    }

    fun processQueue(): Int {
        val userIds = waitingQueueRepository.popMinN(BATCH_SIZE)
        if (userIds.isEmpty()) return 0

        userIds.forEach { userIdStr ->
            val userId = userIdStr.toLong()
            val token = java.util.UUID.randomUUID().toString()
            entryTokenRepository.issueToken(userId, token, TOKEN_TTL_SECONDS)
        }

        return userIds.size
    }

    fun validateEntryToken(userId: Long): String {
        return entryTokenRepository.getToken(userId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "유효한 입장 토큰이 없습니다. 대기열을 통해 입장해주세요.")
    }

    fun consumeEntryToken(userId: Long) {
        entryTokenRepository.deleteToken(userId)
    }

    private fun calculateEstimatedWaitSeconds(position: Long): Long {
        if (position <= 0) return 0
        return (position / THROUGHPUT_PER_SECOND).toLong()
    }
}
