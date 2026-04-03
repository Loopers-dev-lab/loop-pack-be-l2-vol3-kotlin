package com.loopers.application.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.WaitingQueueRepository
import org.springframework.stereotype.Component

@Component
class EnterQueueUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
) {
    companion object {
        /**
         * 유저당 예상 처리 시간(초).
         * DB 커넥션 풀 50, 평균 처리 200ms → 175 TPS → 유저당 약 1/175초 ≈ 0.006초
         * 대기열 앞 유저 1명당 약 0.006초 대기로 산정하되, 스케줄러 100ms 주기 고려하여 반올림.
         */
        private const val SECONDS_PER_USER = 0.006
    }

    fun enter(userId: Long): QueueEntryResult {
        val existingToken = entryTokenRepository.findToken(userId)
        if (existingToken != null) {
            return QueueEntryResult.alreadyAuthorized(existingToken)
        }

        val score = System.currentTimeMillis().toDouble()
        waitingQueueRepository.enqueue(userId, score)

        val position = (waitingQueueRepository.getPosition(userId) ?: 0L) + 1
        val estimatedWaitSeconds = (position * SECONDS_PER_USER).toLong().coerceAtLeast(1)

        return QueueEntryResult.queued(position, estimatedWaitSeconds)
    }
}
