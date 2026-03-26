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
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Duration

@SpringBootTest
class OutboxEventPublisherIntegrationTest @Autowired constructor(
    private val outboxEventJpaRepository: OutboxEventJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

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

        // act & assert — @Scheduled가 1초 간격으로 실행되므로 대기
        await atMost Duration.ofSeconds(10) untilAsserted {
            val updated = outboxEventJpaRepository.findById(outboxEvent.id).get()
            assertThat(updated.status).isEqualTo(OutboxStatus.SENT)
            assertThat(updated.sentAt).isNotNull()
        }
    }
}
