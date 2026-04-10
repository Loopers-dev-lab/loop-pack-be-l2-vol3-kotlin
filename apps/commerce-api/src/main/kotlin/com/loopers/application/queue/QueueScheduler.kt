package com.loopers.application.queue

import com.loopers.domain.queue.WaitingQueueService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["queue.scheduler.enabled"], havingValue = "true", matchIfMissing = true)
class QueueScheduler(
    private val waitingQueueService: WaitingQueueService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${queue.scheduler.fixed-delay-ms:100}")
    fun processWaitingQueue() {
        val processedCount = waitingQueueService.processQueue()
        if (processedCount > 0) {
            log.info("대기열 처리 완료: {}명에게 입장 토큰 발급", processedCount)
        }
    }
}
