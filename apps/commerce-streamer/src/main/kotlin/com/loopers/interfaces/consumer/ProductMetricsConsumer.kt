package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.KafkaTopicConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ProductMetricsConsumer(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopicConfig.PRODUCT_ACTION_TOPIC],
        containerFactory = KafkaConfig.SINGLE_LISTENER,
        groupId = "commerce-streamer-metrics",
    )
    fun onProductAction(
        record: ConsumerRecord<String, ByteArray>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val eventId = record.headers().lastHeader("event-id")?.value()?.let { String(it) }

            if (eventId != null && !tryMarkConsumed(eventId)) {
                log.debug("이미 처리된 이벤트 스킵: eventId={}", eventId)
                acknowledgment.acknowledge()
                return
            }

            val payload = objectMapper.readValue(record.value(), Map::class.java)
            val actionType = payload["actionType"] as? String
            val targetId = (payload["targetId"] as? Number)?.toLong()

            if (actionType == null || targetId == null) {
                log.warn("메트릭 이벤트 필수 필드 누락: record={}", String(record.value()))
                acknowledgment.acknowledge()
                return
            }

            val viewDelta = if (actionType == "VIEW") 1 else 0
            val likeDelta = if (actionType == "LIKE") 1 else 0
            val orderDelta = if (actionType == "ORDER") 1 else 0

            jdbcTemplate.update(
                """
                INSERT INTO product_metrics (product_id, view_count, like_count, order_count, version, updated_at)
                VALUES (?, ?, ?, ?, 1, NOW())
                ON DUPLICATE KEY UPDATE
                    view_count  = view_count  + VALUES(view_count),
                    like_count  = like_count  + VALUES(like_count),
                    order_count = order_count + VALUES(order_count),
                    version     = version + 1,
                    updated_at  = NOW()
                """.trimIndent(),
                targetId,
                viewDelta,
                likeDelta,
                orderDelta,
            )

            log.debug("상품 메트릭 업데이트: productId={}, action={}", targetId, actionType)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("상품 메트릭 처리 실패: record={}, error={}", String(record.value()), e.message, e)
        }
    }

    private fun tryMarkConsumed(eventId: String): Boolean {
        val inserted = jdbcTemplate.update(
            "INSERT IGNORE INTO kafka_consumed_event (event_id, consumer_group, handled_at) VALUES (?, ?, NOW())",
            eventId,
            CONSUMER_GROUP,
        )
        return inserted > 0
    }

    companion object {
        const val CONSUMER_GROUP = "commerce-streamer-metrics"
    }
}
