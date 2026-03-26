package com.loopers.interfaces.consumer

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.application.metrics.MetricsEventProcessor
import com.loopers.config.kafka.KafkaConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    private val metricsEventProcessor: MetricsEventProcessor,
    private val objectMapper: ObjectMapper,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val EVENT_TYPE_ORDER_CREATED = "ORDER_CREATED"
        private const val EVENT_TYPE_PAYMENT_APPROVED = "PAYMENT_APPROVED"
        private const val EVENT_TYPE_PAYMENT_FAILED = "PAYMENT_FAILED"
    }

    @KafkaListener(
        topics = ["order-events"],
        groupId = "commerce-streamer-order",
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(
        messages: List<ConsumerRecord<String, String>>,
        acknowledgment: Acknowledgment,
    ) {
        messages.forEach { record ->
            try {
                val eventId = record.headerValue("eventId")
                val eventType = record.headerValue("eventType")

                if (eventId == null || eventType == null) {
                    log.warn("이벤트 헤더 누락 [topic={}, offset={}]", record.topic(), record.offset())
                    return@forEach
                }

                val payload = objectMapper.readTree(record.value())

                when (eventType) {
                    EVENT_TYPE_ORDER_CREATED -> {
                        val productIds = payload.get("productIds").map { it.asLong() }
                        val totalAmount = payload.get("totalAmount").asLong()
                        metricsEventProcessor.processOrderCreated(eventId, eventType, productIds, totalAmount)
                    }
                    EVENT_TYPE_PAYMENT_APPROVED -> {
                        val productIds = payload.get("productIds").map { it.asLong() }
                        val amount = payload.get("amount").asLong()
                        metricsEventProcessor.processPaymentApproved(eventId, eventType, productIds, amount)
                    }
                    EVENT_TYPE_PAYMENT_FAILED -> {
                        log.info(
                            "결제 실패 이벤트 수신 [eventId={}, orderId={}]",
                            eventId,
                            payload.get("orderId").asLong(),
                        )
                    }
                    else -> log.warn("알 수 없는 이벤트 타입 [eventType={}]", eventType)
                }
            } catch (e: Exception) {
                log.error("order 이벤트 처리 실패 [offset={}]", record.offset(), e)
            }
        }
        acknowledgment.acknowledge()
    }

    private fun ConsumerRecord<String, String>.headerValue(key: String): String? {
        return headers().lastHeader(key)?.value()?.let { String(it) }
    }
}
