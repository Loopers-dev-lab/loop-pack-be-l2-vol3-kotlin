package com.loopers.infrastructure.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * Dead Letter Queue 발행기.
 *
 * Consumer에서 재시도 소진 후에도 실패한 메시지를 DLQ 토픽으로 발행한다.
 * DLQ 토픽 네이밍: "{원본 토픽}.DLQ"
 *
 * 운영자가 DLQ 토픽을 모니터링하여 수동 재처리하거나,
 * 별도 DLQ Consumer로 자동 재처리할 수 있다.
 */
@Component
class DeadLetterPublisher(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
) {

    companion object {
        private val log = LoggerFactory.getLogger(DeadLetterPublisher::class.java)
        private const val DLQ_SUFFIX = ".DLQ"
    }

    fun publish(record: ConsumerRecord<String, String>, exception: Exception) {
        val dlqTopic = record.topic() + DLQ_SUFFIX
        try {
            kafkaTemplate.send(dlqTopic, record.key(), record.value())
            log.warn(
                "DLQ 발행 [topic={}, key={}, offset={}, error={}]",
                dlqTopic,
                record.key(),
                record.offset(),
                exception.message,
            )
        } catch (e: Exception) {
            log.error(
                "DLQ 발행마저 실패 — 수동 처리 필요 [topic={}, key={}, offset={}]",
                record.topic(),
                record.key(),
                record.offset(),
                e,
            )
        }
    }
}
