package com.loopers.infrastructure.queue

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.config.kafka.KafkaConfig
import com.loopers.kafka.KafkaTopics
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class QueueAdmissionConsumer(
    private val objectMapper: ObjectMapper,
    private val queueRedisSupport: QueueRedisSupport,
) {
    companion object {
        private const val GROUP_ID = "queue-admission-consumer"
    }

    @KafkaListener(
        topics = [KafkaTopics.ORDER_ENTRY_ADMISSION_EVENTS],
        groupId = GROUP_ID,
        containerFactory = KafkaConfig.MANUAL_LISTENER,
    )
    fun consume(message: String, acknowledgment: Acknowledgment) {
        val payload = objectMapper.readValue(message, KafkaQueueAdmissionMessage::class.java)
        queueRedisSupport.issueToken(payload.strategy, payload.memberId, payload.token)
        acknowledgment.acknowledge()
    }
}
