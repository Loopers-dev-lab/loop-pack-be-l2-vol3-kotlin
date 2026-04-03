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
class GetQueuePositionUseCase(
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

            // 1. 토큰 보유 여부 확인 (대기열에서 이미 빠진 유저일 수 있음)
            val token = entryTokenRepository.find(userIdVo)
            if (token != null) {
                return QueuePositionInfo.fromToken(token)
            }

            // 2. 순번 조회 — 없으면 404
            val position = waitingQueueRepository.findPosition(userIdVo)
                ?: throw CoreException(ErrorType.NOT_FOUND, "대기열에 등록되지 않은 사용자입니다.")

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
