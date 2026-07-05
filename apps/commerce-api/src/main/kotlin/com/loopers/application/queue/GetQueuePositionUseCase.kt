package com.loopers.application.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.QueueThroughput
import com.loopers.domain.queue.WaitingQueueRepository
import org.springframework.stereotype.Component

@Component
class GetQueuePositionUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
) {
    fun getPosition(userId: Long): QueuePositionResult {
        val token = entryTokenRepository.findToken(userId)
        if (token != null) {
            return QueuePositionResult.authorized(token)
        }

        // size를 rank보다 먼저 읽는다: 두 읽기 사이에 스케줄러가 dequeue해도
        // 먼저 읽은 size가 더 크므로 position > totalWaiting 모순이 생기지 않는다.
        // 반대 방향(사이에 enqueue 증가)은 클램프로 막는다.
        val queueSize = waitingQueueRepository.getQueueSize()
        val rank = waitingQueueRepository.getPosition(userId)
            ?: return QueuePositionResult.notInQueue(queueSize)

        val position = rank + 1
        val totalWaiting = queueSize.coerceAtLeast(position)

        return QueuePositionResult.waiting(position, QueueThroughput.estimateWaitSeconds(position), totalWaiting)
    }
}
