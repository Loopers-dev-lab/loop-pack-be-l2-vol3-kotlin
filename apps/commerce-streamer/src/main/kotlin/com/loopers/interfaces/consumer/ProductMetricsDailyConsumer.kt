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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Component
class ProductMetricsDailyConsumer(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopicConfig.PRODUCT_ACTION_TOPIC],
        containerFactory = KafkaConfig.BATCH_LISTENER,
        groupId = CONSUMER_GROUP,
    )
    fun onProductActions(
        records: List<ConsumerRecord<String, ByteArray>>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val deltaMap = mutableMapOf<Pair<Long, LocalDate>, MetricsDelta>()

            for (record in records) {
                val eventId = extractEventId(record)
                if (eventId != null && !tryMarkConsumed(eventId)) {
                    log.debug("이미 처리된 이벤트 스킵: eventId={}", eventId)
                    continue
                }

                val payload = parseRecord(record) ?: continue
                val targetId = payload.targetId ?: continue
                val metricDate = resolveMetricDate(record)
                val key = Pair(targetId, metricDate)
                val current = deltaMap.getOrDefault(key, MetricsDelta())

                when (payload.actionType) {
                    "VIEW" -> deltaMap[key] = current.copy(viewDelta = current.viewDelta + 1)
                    "LIKE" -> deltaMap[key] = current.copy(likeDelta = current.likeDelta + 1)
                    "ORDER" -> {
                        val price = payload.price
                        val quantity = payload.quantity
                        if (price == null || quantity == null) {
                            log.warn("ORDER price/quantity 누락, order 스킵: targetId={}", targetId)
                        } else {
                            deltaMap[key] = current.copy(
                                orderDelta = current.orderDelta + 1,
                                orderAmountDelta = current.orderAmountDelta + price * quantity,
                            )
                        }
                    }
                    else -> {}
                }
            }

            if (deltaMap.isNotEmpty()) {
                flushDeltas(deltaMap)
            }

            log.debug(
                "일별 메트릭 갱신 완료: records={}, products={}",
                records.size,
                deltaMap.size,
            )
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("일별 메트릭 처리 실패 (ack 미수행, 재전달 예정): batchSize={}, error={}", records.size, e.message, e)
        }
    }

    private fun extractEventId(record: ConsumerRecord<String, ByteArray>): String? =
        record.headers().lastHeader("event-id")?.value()?.let { String(it) }

    private fun resolveMetricDate(record: ConsumerRecord<String, ByteArray>): LocalDate {
        val ts = record.timestamp()
        return if (ts > 0L) {
            Instant.ofEpochMilli(ts).atZone(ZONE_ID).toLocalDate()
        } else {
            LocalDate.now(ZONE_ID)
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

    private fun parseRecord(record: ConsumerRecord<String, ByteArray>): ProductActionPayload? {
        return try {
            val payload = objectMapper.readValue(record.value(), ProductActionPayload::class.java)
            if (payload.actionType == null || payload.targetId == null) return null
            payload
        } catch (e: Exception) {
            log.warn("일별 메트릭 이벤트 파싱 실패: offset={}, partition={}", record.offset(), record.partition(), e)
            null
        }
    }

    private fun flushDeltas(deltaMap: Map<Pair<Long, LocalDate>, MetricsDelta>) {
        val entries = deltaMap.entries.toList()
        jdbcTemplate.batchUpdate(
            """
            INSERT INTO product_metrics_daily
                (product_id, metric_date, view_count, like_count, order_count, order_amount_sum, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, NOW(6))
            ON DUPLICATE KEY UPDATE
                view_count       = view_count       + VALUES(view_count),
                like_count       = like_count       + VALUES(like_count),
                order_count      = order_count      + VALUES(order_count),
                order_amount_sum = order_amount_sum + VALUES(order_amount_sum),
                updated_at       = NOW(6)
            """.trimIndent(),
            entries.map { (key, delta) ->
                arrayOf(
                    key.first,
                    key.second,
                    delta.viewDelta,
                    delta.likeDelta,
                    delta.orderDelta,
                    delta.orderAmountDelta,
                )
            },
        )
    }

    private data class MetricsDelta(
        val viewDelta: Long = 0,
        val likeDelta: Long = 0,
        val orderDelta: Long = 0,
        val orderAmountDelta: Long = 0,
    )

    private data class ProductActionPayload(
        val memberId: Long? = null,
        val actionType: String? = null,
        val targetType: String? = null,
        val targetId: Long? = null,
        val price: Long? = null,
        val quantity: Int? = null,
    )

    companion object {
        const val CONSUMER_GROUP = "commerce-streamer-metrics-daily"
        private val ZONE_ID = ZoneId.of("Asia/Seoul")
    }
}
