package com.loopers.infrastructure.queue

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.queue.OrderEntryQueueStrategy
import com.loopers.application.queue.QueueInfo
import com.loopers.application.queue.QueueStrategyType
import com.loopers.kafka.KafkaTopics
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class KafkaOnlyOrderEntryQueueStrategy(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper,
    private val queueDbSupport: QueueDbSupport,
) : OrderEntryQueueStrategy {
    companion object {
        private const val ORDERING_KEY = "order-entry-kafka-only"
    }

    override val type: QueueStrategyType = QueueStrategyType.KAFKA_ONLY

    override fun enter(memberId: Long): QueueInfo.Status {
        val payload = objectMapper.writeValueAsString(KafkaQueueEnterMessage(strategy = type, memberId = memberId))
        val metadata = kafkaTemplate.send(KafkaTopics.ORDER_ENTRY_EVENTS, ORDERING_KEY, payload).get().recordMetadata
        return queueDbSupport.enter(type, memberId, metadata.offset())
    }

    override fun getStatus(memberId: Long): QueueInfo.Status = queueDbSupport.getStatus(type, memberId)

    override fun admit(batchSize: Int): Int = queueDbSupport.admit(type, batchSize)

    override fun validateToken(memberId: Long, token: String) {
        queueDbSupport.validateToken(type, memberId, token)
    }

    override fun complete(memberId: Long, token: String) {
        queueDbSupport.complete(type, memberId, token)
    }
}
