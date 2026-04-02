package com.loopers.application.queue

import com.loopers.domain.event.OrderCreatedEvent
import com.loopers.domain.queue.OrderQueueStore
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QueueTokenEventListener(
    private val orderQueueStore: OrderQueueStore,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async("eventExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleOrderCreated(event: OrderCreatedEvent) {
        try {
            orderQueueStore.deleteToken(event.userId)
            log.info("주문 완료 후 대기열 토큰 삭제 [userId={}, orderId={}]", event.userId, event.orderId)
        } catch (e: Exception) {
            log.warn("대기열 토큰 삭제 실패 — TTL 만료로 자동 정리됨 [userId={}]", event.userId, e)
        }
    }
}
