package com.loopers.interfaces.support.scheduler

import com.loopers.application.queue.IssueEntryTokensUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class QueueScheduler(
    private val issueEntryTokensUseCase: IssueEntryTokensUseCase,
) {

    @Scheduled(fixedDelayString = "\${queue.scheduler-delay-ms}")
    fun issueTokens() {
        issueEntryTokensUseCase.execute()
    }
}
