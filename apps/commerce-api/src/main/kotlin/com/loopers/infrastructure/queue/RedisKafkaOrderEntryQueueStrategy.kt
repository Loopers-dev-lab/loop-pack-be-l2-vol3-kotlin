package com.loopers.infrastructure.queue

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.queue.OrderEntryQueueStrategy
import com.loopers.application.queue.QueueInfo
import com.loopers.application.queue.QueueStrategyType
import com.loopers.kafka.KafkaTopics
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class RedisKafkaOrderEntryQueueStrategy(
    private val queueRedisSupport: QueueRedisSupport,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) : OrderEntryQueueStrategy {
    override val type: QueueStrategyType = QueueStrategyType.REDIS_KAFKA

    override fun enter(memberId: Long): QueueInfo.Status = queueRedisSupport.enter(type, memberId)

    override fun getStatus(memberId: Long): QueueInfo.Status = queueRedisSupport.getStatus(type, memberId)

    override fun admit(batchSize: Int): Int {
        return queueRedisSupport.admit(type, batchSize) { memberId, token, expiresAt ->
            val message = KafkaQueueAdmissionMessage(
                strategy = type,
                memberId = memberId,
                token = token,
                expiresAt = expiresAt.toString(),
            )
            kafkaTemplate.send(
                KafkaTopics.ORDER_ENTRY_ADMISSION_EVENTS,
                memberId.toString(),
                objectMapper.writeValueAsString(message),
            ).get()
            queueRedisSupport.issueToken(type, memberId, token, expiresAt)
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
