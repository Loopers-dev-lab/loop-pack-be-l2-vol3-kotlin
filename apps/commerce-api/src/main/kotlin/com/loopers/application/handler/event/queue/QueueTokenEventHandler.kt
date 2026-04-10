package com.loopers.application.handler.event.queue

import com.loopers.application.handler.command.queue.ConsumeQueueTokenCommandHandler
import com.loopers.domain.common.event.OrderRequestedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class QueueTokenEventHandler(
    private val consumeQueueTokenCommandHandler: ConsumeQueueTokenCommandHandler,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: OrderRequestedEvent) {
        consumeQueueTokenCommandHandler.handle(event.memberId)
    }
}
