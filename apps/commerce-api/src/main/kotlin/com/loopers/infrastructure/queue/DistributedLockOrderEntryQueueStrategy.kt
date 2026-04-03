package com.loopers.infrastructure.queue

import com.loopers.application.queue.OrderEntryQueueStrategy
import com.loopers.application.queue.QueueInfo
import com.loopers.application.queue.QueueStrategyType
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class DistributedLockOrderEntryQueueStrategy(
    private val queueDbSupport: QueueDbSupport,
    private val queueSequenceAllocator: QueueSequenceAllocator,
    private val redisDistributedLockExecutor: RedisDistributedLockExecutor,
) : OrderEntryQueueStrategy {
    override val type: QueueStrategyType = QueueStrategyType.DISTRIBUTED_LOCK

    override fun enter(memberId: Long): QueueInfo.Status {
        return redisDistributedLockExecutor.execute("queue:lock:enter:${type.name}:$memberId", Duration.ofSeconds(3)) {
            queueDbSupport.enter(type, memberId, queueSequenceAllocator.next(type))
        } ?: throw CoreException(ErrorType.QUEUE_LOCK_NOT_ACQUIRED)
    }

    override fun getStatus(memberId: Long): QueueInfo.Status = queueDbSupport.getStatus(type, memberId)

    override fun admit(batchSize: Int): Int {
        return redisDistributedLockExecutor.execute("queue:lock:admit:${type.name}", Duration.ofSeconds(3)) {
            queueDbSupport.admit(type, batchSize)
        } ?: 0
    }

    override fun validateToken(memberId: Long, token: String) {
        queueDbSupport.validateToken(type, memberId, token)
    }

    override fun complete(memberId: Long, token: String) {
        queueDbSupport.complete(type, memberId, token)
    }
}
