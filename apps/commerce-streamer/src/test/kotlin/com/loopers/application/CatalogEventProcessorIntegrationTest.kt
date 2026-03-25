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

@SpringBootTest
class CatalogEventProcessorIntegrationTest @Autowired constructor(
    private val catalogEventProcessor: CatalogEventProcessor,
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
            assertThat(metrics).isNotNull
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
            assertThat(metrics).isNotNull
            assertThat(metrics!!.likeCount).isEqualTo(1)
        }
    }
}
