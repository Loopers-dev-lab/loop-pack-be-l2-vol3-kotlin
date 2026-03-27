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
class PaymentFailedConsumer(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopicConfig.PAYMENT_FAILED_TOPIC],
        containerFactory = KafkaConfig.SINGLE_LISTENER,
        groupId = CONSUMER_GROUP,
    )
    fun onPaymentFailed(
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
            val orderId = (payload["orderId"] as Number).toLong()
            log.info("결제 실패 보상 메시지 수신: orderId={}", orderId)

            // 1. 주문 정보 조회
            val orderItems = jdbcTemplate.queryForList(
                "SELECT product_id, quantity FROM order_item WHERE order_id = ? ORDER BY product_id ASC",
                orderId,
            )

            val couponId = jdbcTemplate.queryForObject(
                "SELECT coupon_id FROM orders WHERE id = ?",
                Long::class.java,
                orderId,
            )

            // 2. 재고 복원 (productId 오름차순 — 데드락 방지)
            orderItems.forEach { item ->
                val productId = (item["product_id"] as Number).toLong()
                val quantity = (item["quantity"] as Number).toInt()
                jdbcTemplate.update(
                    "UPDATE product SET stock_quantity = stock_quantity + ? WHERE id = ?",
                    quantity,
                    productId,
                )
            }

            // 3. 쿠폰 복원
            if (couponId != null) {
                jdbcTemplate.update(
                    "UPDATE issued_coupon SET status = 'AVAILABLE', used_at = NULL WHERE id = ? AND status = 'USED'",
                    couponId,
                )
            }

            // 4. 주문 취소
            jdbcTemplate.update(
                "UPDATE orders SET status = 'CANCELLED', updated_at = NOW() WHERE id = ? AND status = 'PAYMENT_PENDING'",
                orderId,
            )

            log.info("결제 실패 보상 완료: orderId={}", orderId)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("결제 실패 보상 처리 실패: error={}", e.message, e)
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
        const val CONSUMER_GROUP = "commerce-streamer-payment"
    }
}
