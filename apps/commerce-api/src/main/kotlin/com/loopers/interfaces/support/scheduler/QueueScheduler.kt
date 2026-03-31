package com.loopers.interfaces.support.scheduler

import com.loopers.application.queue.GetQueuePositionUseCase
import com.loopers.application.queue.IssueEntryTokensUseCase
import com.loopers.application.queue.QueueFallbackHandler
import com.loopers.interfaces.api.queue.dto.QueueV1Dto
import com.loopers.interfaces.support.sse.QueueSseEmitterRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class QueueScheduler(
    private val issueEntryTokensUseCase: IssueEntryTokensUseCase,
    private val getQueuePositionUseCase: GetQueuePositionUseCase,
    private val queueSseEmitterRegistry: QueueSseEmitterRegistry,
    private val queueFallbackHandler: QueueFallbackHandler,
) {

    @Scheduled(fixedDelayString = "\${queue.scheduler-delay-ms}")
    fun issueTokens() {
        val issuedTokens = try {
            val tokens = issueEntryTokensUseCase.execute()
            queueFallbackHandler.markAvailable()
            tokens
        } catch (e: Exception) {
            queueFallbackHandler.markUnavailable(e.message ?: "Redis 연결 실패")
            return
        }

        if (issuedTokens.isEmpty()) return

        // 토큰 발급된 유저에게 SSE 이벤트 push 후 연결 종료
        issuedTokens.forEach { issued ->
            val response = QueueV1Dto.QueuePositionResponse(
                position = 0,
                estimatedWaitSeconds = 0,
                token = issued.token,
                recommendedPollIntervalMs = 0,
            )
            queueSseEmitterRegistry.sendEvent(issued.userId, "token-issued", response)
            queueSseEmitterRegistry.complete(issued.userId)
        }

        // 나머지 연결 유저에게 순번 업데이트 push
        queueSseEmitterRegistry.connectedUserIds().forEach { userId ->
            try {
                val positionInfo = getQueuePositionUseCase.execute(userId)
                val response = QueueV1Dto.QueuePositionResponse.from(positionInfo)
                queueSseEmitterRegistry.sendEvent(userId, "position", response)
            } catch (_: Exception) {
                // 대기열에서 이미 빠진 유저는 무시
            }
        }
    }
}
