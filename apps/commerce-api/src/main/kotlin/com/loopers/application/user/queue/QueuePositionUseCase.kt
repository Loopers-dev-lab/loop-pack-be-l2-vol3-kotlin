package com.loopers.application.user.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.WaitingQueueRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Service

@Service
class QueuePositionUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
) {
    fun getPosition(command: QueueCommand.Position): QueueResult.Position {
        val existingToken = entryTokenRepository.findByUserId(command.userId)
        if (existingToken != null) {
            return QueueResult.Position.Ready(
                tokenExpiresInSeconds = existingToken.remainingSeconds,
            )
        }

        val queuePosition = waitingQueueRepository.getPosition(command.userId)
            ?: throw CoreException(ErrorType.QUEUE_ENTRY_NOT_FOUND)

        return QueueResult.Position.Waiting(
            position = queuePosition.position,
            estimatedWaitSeconds = QueueCalculator.estimateWaitSeconds(queuePosition.position),
            totalWaiting = queuePosition.totalWaiting,
            retryAfterMs = QueueCalculator.calculateRetryAfterMs(queuePosition.position),
        )
    }
}
