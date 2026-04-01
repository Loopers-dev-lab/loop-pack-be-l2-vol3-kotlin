package com.loopers.infrastructure.orderqueue

import com.loopers.domain.orderqueue.OrderQueueService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class OrderQueueTokenScheduler(
    private val orderQueueService: OrderQueueService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val BATCH_SIZE = 7L
    }

    @Scheduled(fixedRate = 100)
    fun issueTokens() {
        try {
            orderQueueService.processTokenIssuance(BATCH_SIZE)
        } catch (e: Exception) {
            log.warn("주문 대기열 토큰 발급 실패: {}", e.message)
        }
    }
}
