package com.loopers.infrastructure.outbox

import com.loopers.domain.outbox.OutboxEvent
import com.loopers.domain.outbox.OutboxStatus
import com.loopers.event.KafkaTopics
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Duration

/**
 * Kafka 설정 검증 통합 테스트.
 * 로컬 Docker Compose Kafka와 연동하여 실제 Outbox → Kafka 발행 흐름을 검증한다.
 */
@SpringBootTest
class KafkaIdempotencyIntegrationTest @Autowired constructor(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("Outbox → Kafka 발행 검증")
    @Nested
    inner class OutboxPublish {

        @DisplayName("PENDING 상태의 Outbox 이벤트가 Kafka로 발행되면 SENT로 변경된다.")
        @Test
        fun publishesPendingEvent_andMarksSent() {
            // arrange
            val outboxEvent = outboxEventJpaRepository.save(
                OutboxEvent(
                    aggregateType = "PRODUCT",
                    aggregateId = "1",
                    eventType = "PRODUCT_LIKED",
                    topic = KafkaTopics.CATALOG_EVENTS,
                    partitionKey = "1",
                    payload = """{"userId":1,"productId":1,"liked":true}""",
                ),
            )

            // act & assert — @Scheduled가 1초 간격으로 실행
            await atMost Duration.ofSeconds(10) untilAsserted {
                val updated = outboxEventJpaRepository.findById(outboxEvent.id).get()
                assertThat(updated.status).isEqualTo(OutboxStatus.SENT)
                assertThat(updated.sentAt).isNotNull()
            }
        }

        @DisplayName("같은 eventType의 Outbox 이벤트 2건이 모두 SENT로 전환된다.")
        @Test
        fun publishesMultipleEvents() {
            // arrange
            val event1 = outboxEventJpaRepository.save(
                OutboxEvent(
                    aggregateType = "PRODUCT",
                    aggregateId = "1",
                    eventType = "PRODUCT_LIKED",
                    topic = KafkaTopics.CATALOG_EVENTS,
                    partitionKey = "1",
                    payload = """{"userId":1,"productId":1,"liked":true}""",
                ),
            )
            val event2 = outboxEventJpaRepository.save(
                OutboxEvent(
                    aggregateType = "PRODUCT",
                    aggregateId = "2",
                    eventType = "PRODUCT_LIKED",
                    topic = KafkaTopics.CATALOG_EVENTS,
                    partitionKey = "2",
                    payload = """{"userId":2,"productId":2,"liked":true}""",
                ),
            )

            // act & assert
            await atMost Duration.ofSeconds(10) untilAsserted {
                val updated1 = outboxEventJpaRepository.findById(event1.id).get()
                val updated2 = outboxEventJpaRepository.findById(event2.id).get()
                assertThat(updated1.status).isEqualTo(OutboxStatus.SENT)
                assertThat(updated2.status).isEqualTo(OutboxStatus.SENT)
            }
        }
    }
}
