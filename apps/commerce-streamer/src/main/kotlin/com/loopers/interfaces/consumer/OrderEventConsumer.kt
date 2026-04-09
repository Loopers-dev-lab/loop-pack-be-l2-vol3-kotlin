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
class OrderEventConsumer(
    private val idempotencyService: IdempotencyService,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
    private val rankingScoreUpdater: RankingScoreUpdater,
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

    @Suppress("UNCHECKED_CAST")
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

        // [설계 결정] catalog-events와 동일하게 additive 연산이므로 version/updated_at 체크 불필요.
        // 멱등 마킹과 metrics upsert를 같은 트랜잭션으로 묶어 원자성을 보장한다.
        val eventDate = parseEventDate(generic["occurredAt"]?.toString())

        idempotencyService.executeWithIdempotency(eventId, eventType) {
            when (eventType) {
                "ORDER_CREATED" -> {
                    log.info("주문 생성 이벤트 수신. orderId={}", generic["orderId"])
                }
                "ORDER_COMPLETED" -> {
                    val orderId = generic["orderId"]
                    val items = generic["items"] as? List<GenericRecord>
                    if (items.isNullOrEmpty()) {
                        log.info("주문 완료 이벤트 수신 (아이템 없음). orderId={}", orderId)
                        return@executeWithIdempotency
                    }
                    for (item in items) {
                        val productId = (item["productId"] as Number).toLong()
                        val quantity = (item["quantity"] as Number).toInt()
                        val salesAmount = (item["salesAmount"] as Number).toLong()
                        productMetricsJpaRepository.upsertMetrics(
                            productId = productId,
                            viewCount = 0,
                            likeCount = 0,
                            orderCount = quantity.toLong(),
                            salesAmount = salesAmount,
                        )
                        rankingScoreUpdater.incrementScore(
                            productId,
                            RankingEventType.ORDER,
                            eventDate,
                            quantity.toDouble(),
                        )
                    }
                    log.info("주문 완료 metrics 반영. orderId={}, itemCount={}", orderId, items.size)
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
