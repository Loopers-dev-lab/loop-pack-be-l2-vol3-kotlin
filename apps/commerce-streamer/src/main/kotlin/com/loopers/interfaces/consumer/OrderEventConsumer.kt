package com.loopers.interfaces.consumer

import com.loopers.application.event.IdempotencyService
import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.KafkaTopics
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class OrderEventConsumer(
    private val idempotencyService: IdempotencyService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.ORDER_EVENTS],
        groupId = KafkaTopics.GROUP_ORDER_METRICS,
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
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

    private fun processRecord(record: ConsumerRecord<Any, Any>) {
        val generic = record.value() as? GenericRecord
        if (generic == null) {
            log.error(
                "예상과 다른 메시지 타입. topic={}, offset={}, valueType={}",
                record.topic(),
                record.offset(),
                record.value()?.javaClass?.name,
            )
            return
        }
        val eventId = generic["eventId"]?.toString() ?: return
        val eventType = generic["eventType"]?.toString() ?: return

        if (!idempotencyService.tryMarkHandled(eventId, eventType)) {
            log.debug("이미 처리된 이벤트 skip. eventId={}", eventId)
            return
        }

        // [TODO] 향후 order metrics 집계(판매량, 매출액) 추가 예정. 현재는 파이프라인 연결 검증용.
        when (eventType) {
            "ORDER_CREATED" -> {
                log.info("주문 생성 이벤트 수신. orderId={}", generic["orderId"])
            }
            "ORDER_COMPLETED" -> {
                log.info("주문 완료 이벤트 수신. orderId={}", generic["orderId"])
            }
        }
    }
}
