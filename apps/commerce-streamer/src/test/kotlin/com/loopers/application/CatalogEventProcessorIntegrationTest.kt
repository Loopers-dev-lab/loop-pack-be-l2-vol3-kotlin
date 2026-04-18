package com.loopers.application

import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.event.EventEnvelope
import com.loopers.infrastructure.event.EventHandledJpaRepository
import com.loopers.infrastructure.metrics.ProductMetricsDailyJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.LocalDate

@SpringBootTest
class CatalogEventProcessorIntegrationTest @Autowired constructor(
    private val catalogEventProcessor: CatalogEventProcessor,
    private val productMetricsRepository: ProductMetricsRepository,
    private val productMetricsDailyJpaRepository: ProductMetricsDailyJpaRepository,
    private val eventHandledRepository: EventHandledJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun createEnvelope(
        eventId: String = "evt-1",
        eventType: String = "LIKED",
        aggregateId: String = "100",
        version: Long = 1L,
    ) = EventEnvelope(
        eventId = eventId,
        eventType = eventType,
        aggregateId = aggregateId,
        version = version,
        timestamp = Instant.now(),
        payload = """{"userId":1,"productId":100}""",
    )

    @DisplayName("멱등성 통합 테스트:")
    @Nested
    inner class IdempotencyIntegration {

        @DisplayName("같은 eventId를 두 번 처리하면, likeCount는 1만 증가한다.")
        @Test
        fun processesOnlyOnce() {
            // arrange
            val envelope = createEnvelope(eventId = "evt-dedup", version = 1L)

            // act
            catalogEventProcessor.process(envelope)
            catalogEventProcessor.process(envelope)

            // assert
            val metrics = productMetricsRepository.findByProductId(100L)
            assertThat(metrics).isNotNull()
            assertThat(metrics!!.likeCount).isEqualTo(1)
        }
    }

    @DisplayName("최신성 통합 테스트:")
    @Nested
    inner class VersioningIntegration {

        @DisplayName("높은 version 처리 후 낮은 version을 처리하면, 낮은 version은 무시된다.")
        @Test
        fun skipsOlderVersion() {
            // arrange
            val newer = createEnvelope(eventId = "evt-new", version = 100L)
            val older = createEnvelope(eventId = "evt-old", version = 50L)

            // act
            catalogEventProcessor.process(newer)
            catalogEventProcessor.process(older)

            // assert
            val metrics = productMetricsRepository.findByProductId(100L)
            assertThat(metrics).isNotNull()
            assertThat(metrics!!.likeCount).isEqualTo(1)
        }
    }

    @DisplayName("일별 메트릭 적재 통합 테스트:")
    @Nested
    inner class DailyMetricsIntegration {

        @DisplayName("VIEWED 이벤트 처리 시 product_metrics_daily에 오늘 날짜로 viewCount가 적재된다.")
        @Test
        fun viewedEventWritesToDailyMetrics() {
            // arrange
            val envelope = createEnvelope(eventId = "evt-view-1", eventType = "VIEWED", version = 1L)

            // act
            catalogEventProcessor.process(envelope)

            // assert
            val today = LocalDate.now()
            val daily = productMetricsDailyJpaRepository.findByProductIdAndMetricDate(100L, today)
            assertThat(daily).isNotNull
            assertThat(daily!!.viewCount).isEqualTo(1L)
        }

        @DisplayName("LIKED 이벤트 처리 시 product_metrics_daily에 오늘 날짜로 likeCount가 적재된다.")
        @Test
        fun likedEventWritesToDailyMetrics() {
            // arrange
            val envelope = createEnvelope(eventId = "evt-like-1", eventType = "LIKED", version = 1L)

            // act
            catalogEventProcessor.process(envelope)

            // assert
            val today = LocalDate.now()
            val daily = productMetricsDailyJpaRepository.findByProductIdAndMetricDate(100L, today)
            assertThat(daily).isNotNull
            assertThat(daily!!.likeCount).isEqualTo(1L)
        }

        @DisplayName("UNLIKED 이벤트 처리 시 product_metrics_daily에 오늘 날짜로 likeCount가 차감된다.")
        @Test
        fun unlikedEventWritesToDailyMetrics() {
            // arrange
            val likeEnvelope = createEnvelope(eventId = "evt-like-2", eventType = "LIKED", version = 1L)
            val unlikeEnvelope = createEnvelope(eventId = "evt-unlike-1", eventType = "UNLIKED", version = 2L)

            // act
            catalogEventProcessor.process(likeEnvelope)
            catalogEventProcessor.process(unlikeEnvelope)

            // assert
            val today = LocalDate.now()
            val daily = productMetricsDailyJpaRepository.findByProductIdAndMetricDate(100L, today)
            assertThat(daily).isNotNull
            assertThat(daily!!.likeCount).isZero()
        }
    }
}
