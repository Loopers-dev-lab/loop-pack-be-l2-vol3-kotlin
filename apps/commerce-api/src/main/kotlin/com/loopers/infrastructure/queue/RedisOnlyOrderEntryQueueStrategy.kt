package com.loopers.infrastructure.queue

import com.loopers.application.queue.OrderEntryQueueStrategy
import com.loopers.application.queue.QueueInfo
import com.loopers.application.queue.QueueStrategyType
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component

@Component
class RedisOnlyOrderEntryQueueStrategy(
    private val queueRedisSupport: QueueRedisSupport,
) : OrderEntryQueueStrategy {
    override val type: QueueStrategyType = QueueStrategyType.REDIS_ONLY

    override fun enter(memberId: Long): QueueInfo.Status = queueRedisSupport.enter(type, memberId)

    override fun getStatus(memberId: Long): QueueInfo.Status = queueRedisSupport.getStatus(type, memberId)

    override fun admit(batchSize: Int): Int {
        return queueRedisSupport.admit(type, batchSize) { memberId, token, _ ->
            queueRedisSupport.issueToken(type, memberId, token)
        }
    }

    override fun validateToken(memberId: Long, token: String) {
        if (!queueRedisSupport.validateToken(type, memberId, token)) {
            throw CoreException(ErrorType.INVALID_QUEUE_TOKEN)
        }
    }

    override fun complete(memberId: Long, token: String) {
        queueRedisSupport.complete(type, memberId)
    }
}
