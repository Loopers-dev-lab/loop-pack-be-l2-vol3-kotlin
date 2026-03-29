package com.loopers.interfaces.consumer

import com.loopers.config.kafka.KafkaConfig
import com.loopers.domain.metrics.MetricsProcessor
import com.loopers.infrastructure.kafka.DeadLetterPublisher
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * 상품 메트릭 집계 컨슈머.
 *
 * catalog-events, order-events 토픽을 소비하여 product_metrics를 upsert한다.
 * - manual ack: 처리 완료 후에만 offset 커밋
 * - event_handled 기반 멱등: 오프셋 리셋/리밸런싱 시 중복 처리 방지
 * - 건별 트랜잭션: MetricsProcessor가 REQUIRES_NEW로 처리하여 한 건 실패가 다른 건에 영향 없음
 * - DLQ: 처리 실패 시 {원본 토픽}.DLQ로 발행하여 운영자 후처리
 */
@Component
class MetricsConsumer(
    private val metricsProcessor: MetricsProcessor,
    private val deadLetterPublisher: DeadLetterPublisher,
) {

    companion object {
        private val log = LoggerFactory.getLogger(MetricsConsumer::class.java)
        private const val GROUP_ID = "metrics-consumer"
    }

    @KafkaListener(
        topics = ["catalog-events", "order-events"],
        groupId = GROUP_ID,
        containerFactory = KafkaConfig.BATCH_LISTENER,
    )
    fun consume(
        records: List<ConsumerRecord<String, String>>,
        acknowledgment: Acknowledgment,
    ) {
        records.forEach { record ->
            try {
                metricsProcessor.process(record)
            } catch (e: Exception) {
                log.error(
                    "메트릭 처리 실패 → DLQ [topic={}, key={}, offset={}]",
                    record.topic(),
                    record.key(),
                    record.offset(),
                    e,
                )
                deadLetterPublisher.publish(record, e)
            }
        }
        acknowledgment.acknowledge()
    }
}
