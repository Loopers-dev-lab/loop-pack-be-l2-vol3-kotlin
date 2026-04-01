package com.loopers.application.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.domain.queue.waiting.model.QueuePosition
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class EnterQueueUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
    private val queueProperties: QueueProperties,
) {

    fun execute(userId: Long): QueuePositionInfo {
        val userIdVo = UserId(userId)

        // 1. 이미 토큰 보유 시 즉시 반환
        val existingToken = entryTokenRepository.find(userIdVo)
        if (existingToken != null) {
            return QueuePositionInfo(position = 0, estimatedWaitSeconds = 0, token = existingToken)
        }

        // 2. 대기열 진입 (이미 있으면 기존 순번, 상한 초과 시 null)
        val position = waitingQueueRepository.enter(
            userId = userIdVo,
            score = System.currentTimeMillis().toDouble(),
            maxCapacity = queueProperties.maxCapacity,
        ) ?: throw CoreException(ErrorType.TOO_MANY_REQUESTS, "대기열이 가득 찼습니다.")

        // 3. 순번 + 예상 대기 시간 반환
        val queuePosition = QueuePosition.of(position, queueProperties.throughputTps)
        return QueuePositionInfo.from(queuePosition)
    }
}
