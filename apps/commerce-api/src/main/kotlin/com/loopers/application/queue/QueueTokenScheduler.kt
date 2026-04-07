package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueStore
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class QueueTokenScheduler(
    private val orderQueueStore: OrderQueueStore,
    private val queueProperties: QueueProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedRateString = "\${queue.scheduler.interval-ms}")
    fun issueTokens() {
        if (!queueProperties.enabled) return

        val queueSize = orderQueueStore.getQueueSize()
        if (queueSize == 0L) return

        try {
            val issued = orderQueueStore.issueTokens(
                batchSize = queueProperties.scheduler.batchSize,
                ttlSeconds = queueProperties.token.ttlSeconds,
            )
            if (issued > 0) {
                log.info(
                    "대기열 토큰 발급 [issued={}, queueSize={}]",
                    issued,
                    queueSize - issued,
                )
            }
        } catch (e: Exception) {
            log.error("대기열 토큰 발급 실패", e)
        }
    }
}
