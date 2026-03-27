package com.loopers.interfaces.consumer

import com.loopers.application.event.IdempotencyService
import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.KafkaTopics
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class CatalogEventConsumer(
    private val idempotencyService: IdempotencyService,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = [KafkaTopics.CATALOG_EVENTS],
        groupId = KafkaTopics.GROUP_CATALOG_METRICS,
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
                log.error("catalog 이벤트 처리 실패. offset={}, key={}", record.offset(), record.key(), e)
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

        // [설계 결정] version/updated_at 기반 최신 이벤트 필터링은 상태 교체(state replacement) 패턴에서 필요하다.
        // 현재 metrics는 additive(count += N) 연산이므로 모든 이벤트를 순서 무관하게 반영해야 한다.
        // 중복 방지는 event_handled PK 기반 tryMarkHandled()로 보장한다.

        val productId = (generic["productId"] as Number).toLong()

        when (eventType) {
            "PRODUCT_VIEWED" -> {
                productMetricsJpaRepository.upsertMetrics(
                    productId = productId,
                    viewCount = 1,
                    likeCount = 0,
                    orderCount = 0,
                    salesAmount = 0,
                )
            }
            "PRODUCT_LIKED" -> {
                productMetricsJpaRepository.upsertMetrics(
                    productId = productId,
                    viewCount = 0,
                    likeCount = 1,
                    orderCount = 0,
                    salesAmount = 0,
                )
            }
            "PRODUCT_UNLIKED" -> {
                productMetricsJpaRepository.upsertMetrics(
                    productId = productId,
                    viewCount = 0,
                    likeCount = -1,
                    orderCount = 0,
                    salesAmount = 0,
                )
            }
        }
    }
}
