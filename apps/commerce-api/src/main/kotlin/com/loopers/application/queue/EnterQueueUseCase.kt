package com.loopers.application.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.repository.EntryTokenRepository
import com.loopers.domain.queue.waiting.model.EnterResult
import com.loopers.domain.queue.waiting.model.QueuePosition
import com.loopers.domain.queue.waiting.repository.WaitingQueueRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.stereotype.Component

@Component
class EnterQueueUseCase(
    private val waitingQueueRepository: WaitingQueueRepository,
    private val entryTokenRepository: EntryTokenRepository,
    private val queueFallbackHandler: QueueFallbackHandler,
    private val queueProperties: QueueProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(userId: Long): QueuePositionInfo {
        if (!queueFallbackHandler.isAvailable()) {
            throw CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스를 일시적으로 이용할 수 없습니다.")
        }

        return try {
            val userIdVo = UserId(userId)
            val enterResult = waitingQueueRepository.enter(
                userId = userIdVo,
                maxCapacity = queueProperties.maxCapacity,
            )

            when (enterResult) {
                is EnterResult.Entered -> {
                    val queuePosition = QueuePosition.of(enterResult.position, queueProperties.throughputTps)
                    QueuePositionInfo.from(queuePosition)
                }

                EnterResult.AlreadyHasToken -> {
                    val existingToken = entryTokenRepository.find(userIdVo)
                        ?: throw CoreException(ErrorType.INTERNAL_ERROR, "대기열 진입 결과와 토큰 상태가 일치하지 않습니다.")
                    QueuePositionInfo.fromToken(existingToken)
                }

                EnterResult.QueueFull ->
                    throw CoreException(ErrorType.TOO_MANY_REQUESTS, "대기열이 가득 찼습니다.")
            }
        } catch (e: CoreException) {
            throw e
        } catch (e: DataAccessException) {
            log.error("대기열 진입 중 Redis 장애 발생. userId={}", userId, e)
            queueFallbackHandler.markUnavailable()
            throw CoreException(ErrorType.SERVICE_UNAVAILABLE, "대기열 서비스를 일시적으로 이용할 수 없습니다.")
        }
    }
}
