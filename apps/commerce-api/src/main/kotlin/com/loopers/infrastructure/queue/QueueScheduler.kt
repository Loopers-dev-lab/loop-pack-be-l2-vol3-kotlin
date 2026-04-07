package com.loopers.infrastructure.queue

import com.loopers.application.queue.QueueService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["queue.scheduler.enabled"], havingValue = "true", matchIfMissing = true)
class QueueScheduler(
    private val queueService: QueueService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRate = 100)
    fun issueEntryTokens() {
        val issuedCount = queueService.issueTokens(BATCH_SIZE)
        if (issuedCount > 0) {
            log.debug("대기열 토큰 발급: {}명", issuedCount)
        }
    }

    companion object {
        private const val BATCH_SIZE = 18L
    }
}
