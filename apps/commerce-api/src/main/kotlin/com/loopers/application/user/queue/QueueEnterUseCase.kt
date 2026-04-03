package com.loopers.application.user.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.WaitingQueueRepository
import org.springframework.stereotype.Service

@Service
class QueueEnterUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
) {
    fun enter(command: QueueCommand.Enter): QueueResult.Enter {
        val existingToken = entryTokenRepository.findByUserId(command.userId)
        if (existingToken != null) {
            return QueueResult.Enter.Ready(
                token = existingToken.token,
                tokenExpiresInSeconds = existingToken.remainingSeconds,
            )
        }

        val queuePosition = waitingQueueRepository.enter(command.userId)
        return QueueResult.Enter.Waiting(
            position = queuePosition.position,
            estimatedWaitSeconds = QueueCalculator.estimateWaitSeconds(queuePosition.position),
            totalWaiting = queuePosition.totalWaiting,
        )
    }
}
