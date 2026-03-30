package com.loopers.application

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.event.EventEnvelope
import com.loopers.infrastructure.coupon.IssuedCouponJpaRepository
import com.loopers.infrastructure.event.EventHandledJpaRepository
import com.loopers.infrastructure.event.EventLogJpaRepository
import com.loopers.infrastructure.product.ProductStockJpaRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
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

    @Mock
    private lateinit var productStockRepository: ProductStockJpaRepository

    @Mock
    private lateinit var issuedCouponRepository: IssuedCouponJpaRepository

    private val objectMapper = jacksonObjectMapper()

    private val processor by lazy {
        OrderEventProcessor(
            eventHandledRepository,
            eventLogRepository,
            productMetricsRepository,
            productStockRepository,
            issuedCouponRepository,
            objectMapper,
        )
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
            whenever(eventHandledRepository.insertIgnore("evt-duplicate")).thenReturn(0)

            // act
            processor.process(envelope)

            // assert
            verify(productMetricsRepository, never()).incrementSalesCount(any(), any())
            verify(productStockRepository, never()).decrementStock(any(), any())
            verify(issuedCouponRepository, never()).markUsed(any(), any())
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
            whenever(eventHandledRepository.insertIgnore(any())).thenReturn(1)

            // act
            processor.process(envelope)

            // assert
            verify(productMetricsRepository).incrementSalesCount(100L, 2)
            verify(productMetricsRepository).incrementSalesCount(200L, 3)
        }

        @DisplayName("각 주문 항목의 DB 재고를 수량만큼 차감한다.")
        @Test
        fun decrementsStockForEachItem() {
            // arrange
            val payload = """{"orderId":1,"userId":1,"items":[{"productId":100,"quantity":2,"productName":"상품A"},{"productId":200,"quantity":3,"productName":"상품B"}],"couponId":null,"totalAmount":50000,"paymentAmount":50000}"""
            val envelope = createEnvelope(eventId = "evt-stock-1", payload = payload)
            whenever(eventHandledRepository.insertIgnore(any())).thenReturn(1)

            // act
            processor.process(envelope)

            // assert
            verify(productStockRepository).decrementStock(100L, 2)
            verify(productStockRepository).decrementStock(200L, 3)
        }

        @DisplayName("쿠폰이 포함된 주문이면, 쿠폰을 사용 처리한다.")
        @Test
        fun marksCouponAsUsed_whenCouponIncluded() {
            // arrange
            val payload = """{"orderId":1,"userId":42,"items":[{"productId":100,"quantity":1,"productName":"상품A"}],"couponId":5,"totalAmount":10000,"paymentAmount":5000}"""
            val envelope = createEnvelope(eventId = "evt-coupon-1", payload = payload)
            whenever(eventHandledRepository.insertIgnore(any())).thenReturn(1)

            // act
            processor.process(envelope)

            // assert
            verify(issuedCouponRepository).markUsed(eq(5L), eq(42L))
        }

        @DisplayName("쿠폰이 없는 주문이면, 쿠폰 사용 처리를 하지 않는다.")
        @Test
        fun doesNotMarkCoupon_whenNoCoupon() {
            // arrange
            val envelope = createEnvelope(eventId = "evt-no-coupon")
            whenever(eventHandledRepository.insertIgnore(any())).thenReturn(1)

            // act
            processor.process(envelope)

            // assert
            verify(issuedCouponRepository, never()).markUsed(any(), any())
        }

        @DisplayName("처리 완료 후 EventHandled 삽입과 EventLog를 저장한다.")
        @Test
        fun savesEventHandledAndEventLog() {
            // arrange
            val envelope = createEnvelope(eventId = "evt-order-2")
            whenever(eventHandledRepository.insertIgnore(any())).thenReturn(1)

            // act
            processor.process(envelope)

            // assert
            verify(eventHandledRepository).insertIgnore("evt-order-2")
            verify(eventLogRepository).save(any())
        }
    }

    @DisplayName("알 수 없는 이벤트 타입이면, 비즈니스 로직을 실행하지 않는다.")
    @Test
    fun skipsUnknownEventType() {
        // arrange
        val envelope = createEnvelope(eventId = "evt-unknown", eventType = "UNKNOWN_TYPE")
        whenever(eventHandledRepository.insertIgnore(any())).thenReturn(1)

        // act
        processor.process(envelope)

        // assert
        verify(productMetricsRepository, never()).incrementSalesCount(any(), any())
        verify(productStockRepository, never()).decrementStock(any(), any())
    }
}
