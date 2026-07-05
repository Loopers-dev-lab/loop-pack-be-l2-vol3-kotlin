package com.loopers.application.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.QueueThroughput
import com.loopers.domain.queue.WaitingQueueRepository
import org.springframework.stereotype.Component

@Component
class EnterQueueUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
) {
    fun enter(userId: Long): QueueEntryResult {
        val existingToken = entryTokenRepository.findToken(userId)
        if (existingToken != null) {
            return QueueEntryResult.alreadyAuthorized(existingToken)
        }

        val score = System.currentTimeMillis().toDouble()
        waitingQueueRepository.enqueue(userId, score)

        // 복제 지연으로 rank가 아직 안 보이면 방금 진입한 유저이므로 맨 뒤로 추정한다.
        // (rank 폴백 0은 "1번째"라는 거짓 안내가 된다)
        val queueSize = waitingQueueRepository.getQueueSize()
        val rank = waitingQueueRepository.getPosition(userId)
        val position = (rank ?: (queueSize - 1).coerceAtLeast(0)) + 1

        // size와 rank는 별도 읽기라 그 사이에 스케줄러가 dequeue하면 모순될 수 있다.
        // position > totalWaiting인 응답만은 클램프로 막는다.
        val totalWaiting = queueSize.coerceAtLeast(position)

        return QueueEntryResult.queued(position, QueueThroughput.estimateWaitSeconds(position), totalWaiting)
    }
}
