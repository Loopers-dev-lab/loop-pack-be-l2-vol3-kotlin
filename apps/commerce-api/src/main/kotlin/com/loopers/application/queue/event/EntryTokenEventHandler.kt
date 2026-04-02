package com.loopers.application.queue.event

import com.loopers.application.queue.QueueService
import com.loopers.infrastructure.queue.WaitingQueueRedisRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class EntryTokenEventHandler(
    private val waitingQueueRedisRepository: WaitingQueueRedisRepository,
    private val queueService: QueueService,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: EntryTokenConsumedEvent) {
        try {
            waitingQueueRedisRepository.deleteToken(event.userId)
            queueService.incrementConsumedCount()
            log.debug("입장 토큰 소비 완료: userId={}, orderId={}", event.userId, event.orderId)
        } catch (e: Exception) {
            log.warn("입장 토큰 삭제 실패: userId={}, orderId={}", event.userId, event.orderId, e)
        }
    }
}
