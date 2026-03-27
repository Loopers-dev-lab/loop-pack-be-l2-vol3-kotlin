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
import org.springframework.transaction.support.TransactionTemplate

@Component
class PaymentSucceededConsumer(
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopicConfig.PAYMENT_SUCCEEDED_TOPIC],
        containerFactory = KafkaConfig.SINGLE_LISTENER,
        groupId = CONSUMER_GROUP,
    )
    fun onPaymentSucceeded(
        record: ConsumerRecord<String, ByteArray>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val eventId = record.headers().lastHeader("event-id")?.value()?.let { String(it) }

            val payload = objectMapper.readValue(record.value(), Map::class.java)
            val orderId = (payload["orderId"] as Number).toLong()
            log.info("결제 성공 메시지 수신: orderId={}", orderId)

            transactionTemplate.execute {
                if (eventId != null && !tryMarkConsumed(eventId)) {
                    log.debug("이미 처리된 이벤트 스킵: eventId={}", eventId)
                    return@execute
                }

                val updatedRows = jdbcTemplate.update(
                    "UPDATE orders SET status = 'PAID', updated_at = NOW() WHERE id = ? AND status = 'PAYMENT_PENDING'",
                    orderId,
                )

                if (updatedRows > 0) {
                    log.info("주문 PAID 처리 완료: orderId={}", orderId)
                } else {
                    log.warn("주문 상태 변경 불가 (이미 처리됨 또는 존재하지 않음): orderId={}", orderId)
                }
            }

            acknowledgment.acknowledge()
        } catch (e: Exception) {
            log.error("결제 성공 메시지 처리 실패: error={}", e.message, e)
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
