package com.loopers.application

import com.loopers.domain.event.EventHandled
import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.event.EventEnvelope
import com.loopers.infrastructure.event.EventHandledJpaRepository
import com.loopers.infrastructure.event.EventLogJpaRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@DisplayName("CatalogEventProcessor")
class CatalogEventProcessorTest {

    @Mock
    private lateinit var eventHandledRepository: EventHandledJpaRepository

    @Mock
    private lateinit var eventLogRepository: EventLogJpaRepository

    @Mock
    private lateinit var productMetricsRepository: ProductMetricsRepository

    @InjectMocks
    private lateinit var processor: CatalogEventProcessor

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

    @DisplayName("멱등성 체크 시,")
    @Nested
    inner class Idempotency {

        @DisplayName("이미 처리된 eventId이면, 비즈니스 로직을 실행하지 않는다.")
        @Test
        fun skipsAlreadyHandledEvent() {
            // arrange
            val envelope = createEnvelope(eventId = "evt-duplicate")
            whenever(eventHandledRepository.existsById("evt-duplicate")).thenReturn(true)

            // act
            processor.process(envelope)

            // assert
            verify(productMetricsRepository, never()).incrementLikeCount(any(), any())
        }
    }

    @DisplayName("최신성 체크 시,")
    @Nested
    inner class Versioning {

        @DisplayName("현재 version보다 낮은 이벤트이면, 비즈니스 로직을 실행하지 않는다.")
        @Test
        fun skipsOlderVersionEvent() {
            // arrange
            val envelope = createEnvelope(version = 5L)
            whenever(eventHandledRepository.existsById(any())).thenReturn(false)
            whenever(productMetricsRepository.getVersion(100L)).thenReturn(10L)

            // act
            processor.process(envelope)

            // assert
            verify(productMetricsRepository, never()).incrementLikeCount(any(), any())
        }

        @DisplayName("현재 version보다 높은 이벤트이면, 비즈니스 로직을 실행한다.")
        @Test
        fun processesNewerVersionEvent() {
            // arrange
            val envelope = createEnvelope(version = 15L)
            whenever(eventHandledRepository.existsById(any())).thenReturn(false)
            whenever(productMetricsRepository.getVersion(100L)).thenReturn(10L)

            // act
            processor.process(envelope)

            // assert
            verify(productMetricsRepository).incrementLikeCount(100L, 15L)
        }
    }
}
