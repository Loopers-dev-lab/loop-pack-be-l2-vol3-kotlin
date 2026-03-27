package com.loopers.infrastructure.outbox

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class OutboxPoller(
    private val jdbcTemplate: JdbcTemplate,
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 1000)
    fun poll() {
        val events = jdbcTemplate.queryForList(
            """
            SELECT id, topic, partition_key, payload, event_type
            FROM outbox_event
            WHERE published_at IS NULL
            ORDER BY created_at ASC LIMIT 100
            """.trimIndent(),
        )

        if (events.isEmpty()) return

        for (event in events) {
            val id = event["id"] as String
            val topic = event["topic"] as String
            val partitionKey = event["partition_key"] as String
            val payload = event["payload"] as String
            val eventType = event["event_type"] as String

            try {
                val record = ProducerRecord<String, String>(topic, partitionKey, payload)
                record.headers().add("event-id", id.toByteArray())
                record.headers().add("event-type", eventType.toByteArray())

                kafkaTemplate.send(record).get()

                jdbcTemplate.update(
                    "UPDATE outbox_event SET published_at = ? WHERE id = ?",
                    ZonedDateTime.now(),
                    id,
                )
                log.debug("Outbox 이벤트 발행 완료: id={}, topic={}", id, topic)
            } catch (e: Exception) {
                log.error("Outbox 이벤트 발행 실패: id={}, topic={}, error={}", id, topic, e.message, e)
                break
            }
        }
    }
}
