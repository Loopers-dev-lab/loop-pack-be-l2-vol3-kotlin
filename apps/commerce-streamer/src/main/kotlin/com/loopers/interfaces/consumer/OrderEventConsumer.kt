package com.loopers.interfaces.consumer

import com.loopers.application.event.IdempotencyService
import com.loopers.config.kafka.KafkaConfig
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderEventConsumer(
    private val idempotencyService: IdempotencyService,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["order-events"],
        groupId = "streamer-order-metrics",
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    @Transactional
    fun consumeBatch(
        messages: List<ConsumerRecord<Any, Any>>,
        acknowledgment: Acknowledgment,
    ) {
        for (record in messages) {
            try {
                processRecord(record)
            } catch (e: Exception) {
                log.error("order 이벤트 처리 실패. offset={}, key={}", record.offset(), record.key(), e)
            }
        }
        acknowledgment.acknowledge()
    }

    @Suppress("UNCHECKED_CAST")
    private fun processRecord(record: ConsumerRecord<Any, Any>) {
        val payload = record.value() as? Map<String, Any> ?: return
        val eventId = payload["eventId"]?.toString() ?: return
        val eventType = payload["eventType"]?.toString() ?: return

        if (idempotencyService.isAlreadyHandled(eventId)) {
            log.debug("이미 처리된 이벤트 skip. eventId={}", eventId)
            return
        }

        when (eventType) {
            "ORDER_CREATED" -> {
                log.info("주문 생성 이벤트 수신. orderId={}", payload["orderId"])
            }
            "ORDER_COMPLETED" -> {
                log.info("주문 완료 이벤트 수신. orderId={}", payload["orderId"])
            }
        }

        idempotencyService.markHandled(eventId, eventType)
    }
}
