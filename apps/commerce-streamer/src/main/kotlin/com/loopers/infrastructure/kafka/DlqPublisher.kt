package com.loopers.infrastructure.kafka

import com.loopers.config.KafkaTopicConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DlqPublisher(
    private val kafkaTemplate: KafkaTemplate<Any, Any>,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(record: ConsumerRecord<String, String>, exception: Exception) {
        val dlqTopic = KafkaTopicConfig.dlqTopicOf(record.topic())

        val producerRecord = ProducerRecord<Any, Any>(
            dlqTopic,
            null,
            record.key(),
            record.value(),
        )

        // 원본 헤더 복사
        record.headers().forEach { header ->
            producerRecord.headers().add(header)
        }

        // 에러 정보 헤더 추가
        producerRecord.headers().add(RecordHeader("dlq.error.class", exception.javaClass.name.toByteArray()))
        producerRecord.headers().add(RecordHeader("dlq.error.message", (exception.message ?: "").toByteArray()))
        producerRecord.headers().add(RecordHeader("dlq.error.timestamp", Instant.now().toString().toByteArray()))
        producerRecord.headers().add(RecordHeader("dlq.original.topic", record.topic().toByteArray()))
        producerRecord.headers().add(RecordHeader("dlq.original.offset", record.offset().toString().toByteArray()))

        try {
            kafkaTemplate.send(producerRecord).get()
            log.warn(
                "DLQ 전송 완료 [topic={}, offset={}, dlqTopic={}, error={}]",
                record.topic(),
                record.offset(),
                dlqTopic,
                exception.message,
            )
        } catch (e: Exception) {
            log.error(
                "DLQ 전송 실패 [topic={}, offset={}, dlqTopic={}]",
                record.topic(),
                record.offset(),
                dlqTopic,
                e,
            )
        }
    }
}
