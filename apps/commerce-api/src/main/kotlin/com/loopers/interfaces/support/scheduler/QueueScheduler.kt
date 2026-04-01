package com.loopers.interfaces.support.scheduler

import com.loopers.application.queue.IssueEntryTokensUseCase
import com.loopers.application.queue.QueueFallbackHandler
import org.springframework.dao.DataAccessException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class QueueScheduler(
    private val issueEntryTokensUseCase: IssueEntryTokensUseCase,
    private val queueFallbackHandler: QueueFallbackHandler,
) {

    @Scheduled(fixedDelayString = "\${queue.scheduler-delay-ms}")
    fun issueTokens() {
        try {
            issueEntryTokensUseCase.execute()
            queueFallbackHandler.markAvailable()
        } catch (e: DataAccessException) {
            queueFallbackHandler.markUnavailable(e.message ?: "Redis 연결 실패")
        }
    }
}
