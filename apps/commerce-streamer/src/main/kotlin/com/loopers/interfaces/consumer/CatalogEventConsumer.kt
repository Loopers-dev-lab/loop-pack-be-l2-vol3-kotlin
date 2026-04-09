package com.loopers.interfaces.consumer

import com.loopers.application.event.IdempotencyService
import com.loopers.config.kafka.KafkaConfig
import com.loopers.config.kafka.KafkaTopics
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import com.loopers.infrastructure.ranking.RankingEventType
import com.loopers.infrastructure.ranking.RankingScoreUpdater
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZonedDateTime

@Component
class CatalogEventConsumer(
    private val idempotencyService: IdempotencyService,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
    private val rankingScoreUpdater: RankingScoreUpdater,
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

        // [설계 결정] version/updated_at 기반 최신 이벤트 필터링은 상태 교체(state replacement) 패턴에서 필요하다.
        // 현재 metrics는 additive(count += N) 연산이므로 모든 이벤트를 순서 무관하게 반영해야 한다.
        // 멱등 마킹과 metrics upsert를 같은 트랜잭션으로 묶어 원자성을 보장한다.

        val productId = (generic["productId"] as Number).toLong()
        val eventDate = parseEventDate(generic["occurredAt"]?.toString())

        idempotencyService.executeWithIdempotency(eventId, eventType) {
            when (eventType) {
                "PRODUCT_VIEWED" -> {
                    productMetricsJpaRepository.upsertMetrics(
                        productId = productId,
                        viewCount = 1,
                        likeCount = 0,
                        orderCount = 0,
                        salesAmount = 0,
                    )
                    rankingScoreUpdater.incrementScore(productId, RankingEventType.VIEW, eventDate)
                }
                "PRODUCT_LIKED" -> {
                    productMetricsJpaRepository.upsertMetrics(
                        productId = productId,
                        viewCount = 0,
                        likeCount = 1,
                        orderCount = 0,
                        salesAmount = 0,
                    )
                    rankingScoreUpdater.incrementScore(productId, RankingEventType.LIKE, eventDate)
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

    private fun parseEventDate(occurredAt: String?): LocalDate {
        if (occurredAt == null) return LocalDate.now()
        return try {
            ZonedDateTime.parse(occurredAt).toLocalDate()
        } catch (e: Exception) {
            LocalDate.now()
        }
    }
}
