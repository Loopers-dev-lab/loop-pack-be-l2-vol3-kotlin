package com.loopers.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.loopers.domain.event.EventHandled
import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.event.EventEnvelope
import com.loopers.infrastructure.event.EventHandledJpaRepository
import com.loopers.infrastructure.event.EventLogJpaRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

@ExtendWith(MockitoExtension::class)
@DisplayName("OrderEventProcessor")
class OrderEventProcessorTest {

    @Mock
    private lateinit var eventHandledRepository: EventHandledJpaRepository

    @Mock
    private lateinit var eventLogRepository: EventLogJpaRepository

    @Mock
    private lateinit var productMetricsRepository: ProductMetricsRepository

    private val objectMapper = jacksonObjectMapper()

    private val processor by lazy {
        OrderEventProcessor(eventHandledRepository, eventLogRepository, productMetricsRepository, objectMapper)
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
            verify(productMetricsRepository, never()).incrementSalesCount(any(), any(), any())
        }
    }

    @DisplayName("ORDER_COMPLETED 이벤트 처리 시,")
    @Nested
    inner class OrderCompleted {

        @DisplayName("각 주문 항목의 salesCount를 수량만큼 증가시킨다.")
        @Test
        fun incrementsSalesCountForEachItem() {
            // arrange
            val payload = """{"orderId":1,"userId":1,"items":[{"productId":100,"quantity":2,"productName":"상품A"},{"productId":200,"quantity":3,"productName":"상품B"}],"couponId":null,"totalAmount":50000,"paymentAmount":50000}"""
            val envelope = createEnvelope(eventId = "evt-order-1", payload = payload)
            whenever(eventHandledRepository.existsById(any())).thenReturn(false)

            // act
            processor.process(envelope)

            // assert
            verify(productMetricsRepository).incrementSalesCount(100L, 2, envelope.version)
            verify(productMetricsRepository).incrementSalesCount(200L, 3, envelope.version)
        }

        @DisplayName("처리 완료 후 EventHandled와 EventLog를 저장한다.")
        @Test
        fun savesEventHandledAndEventLog() {
            // arrange
            val envelope = createEnvelope(eventId = "evt-order-2")
            whenever(eventHandledRepository.existsById(any())).thenReturn(false)

            // act
            processor.process(envelope)

            // assert
            verify(eventHandledRepository).save(any<EventHandled>())
            verify(eventLogRepository).save(any())
        }
    }

    @DisplayName("알 수 없는 이벤트 타입이면, salesCount를 증가시키지 않는다.")
    @Test
    fun skipsUnknownEventType() {
        // arrange
        val envelope = createEnvelope(eventId = "evt-unknown", eventType = "UNKNOWN_TYPE")
        whenever(eventHandledRepository.existsById(any())).thenReturn(false)

        // act
        processor.process(envelope)

        // assert
        verify(productMetricsRepository, never()).incrementSalesCount(any(), any(), any())
    }
}
