package com.loopers.application.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.domain.queue.waiting.model.QueuePosition
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component

@Component
class EnterQueueUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
    private val queueFallbackHandler: QueueFallbackHandler,
    private val queueProperties: QueueProperties,
) {

    fun execute(userId: Long): QueuePositionInfo {
        if (!queueFallbackHandler.isAvailable()) {
            throw CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스를 일시적으로 이용할 수 없습니다.")
        }

        return try {
            val userIdVo = UserId(userId)

            // 1. 이미 토큰 보유 시 즉시 반환
            val existingToken = entryTokenRepository.find(userIdVo)
            if (existingToken != null) {
                return QueuePositionInfo(position = 0, estimatedWaitSeconds = 0, token = existingToken)
            }

            // 2. 대기열 진입 (이미 있으면 기존 순번, 상한 초과 시 null)
            // score는 Redis Lua 내부에서 TIME 커맨드로 원자적으로 생성 (Double 정밀도 한계 회피)
            val position = waitingQueueRepository.enter(
                userId = userIdVo,
                maxCapacity = queueProperties.maxCapacity,
            ) ?: throw CoreException(ErrorType.TOO_MANY_REQUESTS, "대기열이 가득 찼습니다.")

            // 3. 순번 + 예상 대기 시간 반환
            val queuePosition = QueuePosition.of(position, queueProperties.throughputTps)
            QueuePositionInfo.from(queuePosition)
        } catch (e: CoreException) {
            throw e
        } catch (e: DataAccessException) {
            queueFallbackHandler.markUnavailable()
            throw CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스를 일시적으로 이용할 수 없습니다.")
        }
    }
}
