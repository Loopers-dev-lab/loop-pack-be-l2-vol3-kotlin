package com.loopers.e2e

import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.event.EventEnvelope
import com.loopers.support.EmbeddedKafkaTestSupport
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant
import java.util.UUID

class LikeEventE2ETest @Autowired constructor(
    private val productMetricsRepository: ProductMetricsRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) : EmbeddedKafkaTestSupport() {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("좋아요 이벤트 E2E:")
    @Nested
    inner class LikeEventFlow {

        @DisplayName("좋아요 이벤트가 Kafka를 통해 product_metrics의 likeCount에 반영된다.")
        @Test
        fun likeEventUpdatesProductMetrics() {
            // arrange
            waitForConsumerAssignment()
            val envelope = EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = "LIKED",
                aggregateId = "100",
                version = System.currentTimeMillis(),
                timestamp = Instant.now(),
                payload = """{"userId":1,"productId":100}""",
            )

            // act
            sendEnvelope("catalog-events", envelope)

            // assert
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1)).untilAsserted {
                val metrics = productMetricsRepository.findByProductId(100L)
                assertThat(metrics).isNotNull
                assertThat(metrics!!.likeCount).isEqualTo(1)
            }
        }
    }
}
