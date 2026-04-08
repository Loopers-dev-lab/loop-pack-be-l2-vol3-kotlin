package com.loopers.infrastructure.queue

import com.loopers.application.queue.OrderEntryQueueStrategy
import com.loopers.application.queue.QueueInfo
import com.loopers.application.queue.QueueStrategyType
import org.springframework.stereotype.Component

@Component
class PessimisticLockOrderEntryQueueStrategy(
    private val queueDbSupport: QueueDbSupport,
    private val queueSequenceAllocator: QueueSequenceAllocator,
) : OrderEntryQueueStrategy {
    override val type: QueueStrategyType = QueueStrategyType.PESSIMISTIC_LOCK

    override fun enter(memberId: Long): QueueInfo.Status = queueDbSupport.enter(type, memberId, queueSequenceAllocator.next(type))

    override fun getStatus(memberId: Long): QueueInfo.Status = queueDbSupport.getStatus(type, memberId)

    override fun admit(batchSize: Int): Int = queueDbSupport.admitWithPessimisticLock(type, batchSize)

    override fun validateToken(memberId: Long, token: String) {
        queueDbSupport.validateToken(type, memberId, token)
    }

    override fun complete(memberId: Long, token: String) {
        queueDbSupport.complete(type, memberId, token)
    }
}
