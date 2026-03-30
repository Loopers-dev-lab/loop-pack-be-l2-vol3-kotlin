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

class OrderEventE2ETest @Autowired constructor(
    private val productMetricsRepository: ProductMetricsRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) : EmbeddedKafkaTestSupport() {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("주문 완료 이벤트 E2E:")
    @Nested
    inner class OrderCompletedEventFlow {

        @DisplayName("주문 완료 이벤트가 Kafka를 통해 각 상품의 salesCount에 반영된다.")
        @Test
        fun orderCompletedEventUpdatesSalesCount() {
            // arrange
            waitForConsumerAssignment()
            val payload = """{"orderId":1,"userId":1,"items":[{"productId":100,"quantity":2,"productName":"상품A"},{"productId":200,"quantity":3,"productName":"상품B"}],"couponId":null,"totalAmount":50000,"paymentAmount":50000}"""
            val envelope = EventEnvelope(
                eventId = UUID.randomUUID().toString(),
                eventType = "ORDER_COMPLETED",
                aggregateId = "1",
                version = System.currentTimeMillis(),
                timestamp = Instant.now(),
                payload = payload,
            )

            // act
            sendEnvelope("order-events", envelope)

            // assert
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofSeconds(1)).untilAsserted {
                val metricsA = productMetricsRepository.findByProductId(100L)
                assertThat(metricsA).isNotNull
                assertThat(metricsA!!.salesCount).isEqualTo(2)

                val metricsB = productMetricsRepository.findByProductId(200L)
                assertThat(metricsB).isNotNull
                assertThat(metricsB!!.salesCount).isEqualTo(3)
            }
        }
    }
}
