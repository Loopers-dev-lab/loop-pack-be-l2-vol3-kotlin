package com.loopers.application

import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.event.EventEnvelope
import com.loopers.infrastructure.event.EventHandledJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

@SpringBootTest
class OrderEventProcessorIntegrationTest @Autowired constructor(
    private val orderEventProcessor: OrderEventProcessor,
    private val productMetricsRepository: ProductMetricsRepository,
    private val eventHandledRepository: EventHandledJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createEnvelope(
        eventId: String = "evt-1",
        eventType: String = "ORDER_COMPLETED",
        aggregateId: String = "1",
        version: Long = 1L,
        payload: String = """{"orderId":1,"userId":1,"items":[{"productId":100,"quantity":2,"productName":"테스트 상품"}],"couponId":null,"totalAmount":20000,"paymentAmount":20000}""",
    ) = EventEnvelope(
        eventId = eventId,
        eventType = eventType,
        aggregateId = aggregateId,
        version = version,
        timestamp = Instant.now(),
        payload = payload,
    )

    @DisplayName("멱등성 통합 테스트:")
    @Nested
    inner class IdempotencyIntegration {

        @DisplayName("같은 eventId를 두 번 처리하면, salesCount는 1회분만 증가한다.")
        @Test
        fun processesOnlyOnce() {
            // arrange
            val envelope = createEnvelope(eventId = "evt-dedup", version = 1L)

            // act
            orderEventProcessor.process(envelope)
            orderEventProcessor.process(envelope)

            // assert
            val metrics = productMetricsRepository.findByProductId(100L)
            assertThat(metrics).isNotNull
            assertThat(metrics!!.salesCount).isEqualTo(2) // quantity=2, 1회만 처리
        }
    }

    @DisplayName("동시성 멱등성 테스트:")
    @Nested
    inner class ConcurrentIdempotency {

        @DisplayName("같은 eventId를 여러 스레드에서 동시에 처리하면, salesCount는 1회분만 증가한다.")
        @Test
        fun processesOnlyOnceUnderConcurrency() {
            // arrange
            val envelope = createEnvelope(eventId = "evt-concurrent", version = 1L)
            val threadCount = 10
            val executor = Executors.newFixedThreadPool(threadCount)
            val latch = CountDownLatch(threadCount)

            // act
            repeat(threadCount) {
                executor.submit {
                    try {
                        orderEventProcessor.process(envelope)
                    } catch (_: Exception) {
                    } finally {
                        latch.countDown()
                    }
                }
            }
            latch.await()
            executor.shutdown()

            // assert
            val metrics = productMetricsRepository.findByProductId(100L)
            assertThat(metrics).isNotNull
            assertThat(metrics!!.salesCount).isEqualTo(2) // quantity=2, 1회만 처리
        }
    }

    @DisplayName("주문 완료 처리 통합 테스트:")
    @Nested
    inner class OrderCompletedIntegration {

        @DisplayName("여러 상품이 포함된 주문을 처리하면, 각 상품의 salesCount가 수량만큼 증가한다.")
        @Test
        fun incrementsSalesCountForMultipleItems() {
            // arrange
            val payload = """{"orderId":1,"userId":1,"items":[{"productId":100,"quantity":2,"productName":"상품A"},{"productId":200,"quantity":3,"productName":"상품B"}],"couponId":null,"totalAmount":50000,"paymentAmount":50000}"""
            val envelope = createEnvelope(eventId = "evt-multi", payload = payload)

            // act
            orderEventProcessor.process(envelope)

            // assert
            val metricsA = productMetricsRepository.findByProductId(100L)
            assertThat(metricsA).isNotNull
            assertThat(metricsA!!.salesCount).isEqualTo(2)

            val metricsB = productMetricsRepository.findByProductId(200L)
            assertThat(metricsB).isNotNull
            assertThat(metricsB!!.salesCount).isEqualTo(3)
        }
    }
}
