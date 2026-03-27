package com.loopers.application.metric

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.loopers.domain.metric.HandledEventRepository
import com.loopers.domain.metric.ProductLikeCountRepository
import com.loopers.domain.metric.ProductMetric
import com.loopers.domain.metric.ProductMetricRepository
import com.loopers.domain.metric.ProcessedPaymentRepository
import com.loopers.infrastructure.outbox.KafkaEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@DisplayName("KafkaMetricEventHandler")
class KafkaMetricEventHandlerTest {
    private val objectMapper = jacksonObjectMapper()
    private val productMetricRepository: ProductMetricRepository = mock()
    private val handledEventRepository: HandledEventRepository = mock()
    private val productLikeCountRepository: ProductLikeCountRepository = mock()
    private val processedPaymentRepository: ProcessedPaymentRepository = mock()
    private val handler = KafkaMetricEventHandler(
        productMetricRepository,
        handledEventRepository,
        productLikeCountRepository,
        processedPaymentRepository,
    )

    @Test
    @DisplayName("catalog event를 product metric으로 반영한다")
    fun handleCatalogEvent() {
        whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
        whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
        whenever(productMetricRepository.save(check { })).thenAnswer { it.arguments[0] as ProductMetric }

        handler.handle(
            topic = "catalog-events",
            envelope = KafkaEventEnvelope(
                eventId = EVENT_ID,
                eventType = KafkaEventType.PRODUCT_DETAIL_VIEWED,
                aggregateId = PRODUCT_ID,
                payload = objectMapper.readTree("""{"productId":100}"""),
            ),
        )

        verify(productMetricRepository).save(
            check { metric ->
                assertThat(metric.productId).isEqualTo(PRODUCT_ID)
                assertThat(metric.viewCount).isEqualTo(1)
                assertThat(metric.catalogEventVersion).isEqualTo(EVENT_ID)
            },
        )
        verify(handledEventRepository).save(
            check { handled ->
                assertThat(handled.eventId).isEqualTo(EVENT_ID)
                assertThat(handled.topic).isEqualTo("catalog-events")
                assertThat(handled.eventType).isEqualTo(KafkaEventType.PRODUCT_DETAIL_VIEWED)
            },
        )
    }

    @Test
    @DisplayName("like event를 product_like row count 기반 스냅샷으로 반영한다")
    fun handleLikeEvent() {
        whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
        whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
        whenever(productLikeCountRepository.countByProductId(PRODUCT_ID)).thenReturn(3)
        whenever(productMetricRepository.save(check { })).thenAnswer { it.arguments[0] as ProductMetric }

        handler.handle(
            topic = "catalog-events",
            envelope = KafkaEventEnvelope(
                eventId = EVENT_ID,
                eventType = KafkaEventType.PRODUCT_LIKE_REGISTERED,
                aggregateId = PRODUCT_ID,
                payload = objectMapper.readTree("""{"productId":100}"""),
            ),
        )

        verify(productLikeCountRepository).countByProductId(PRODUCT_ID)
        verify(productMetricRepository).save(
            check { metric ->
                assertThat(metric.productId).isEqualTo(PRODUCT_ID)
                assertThat(metric.likeCount).isEqualTo(3)
                assertThat(metric.catalogEventVersion).isEqualTo(EVENT_ID)
            },
        )
    }

    @Test
    @DisplayName("이미 처리한 eventId는 무시한다")
    fun skipHandledEvent() {
        whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(true)

        handler.handle(
            topic = "catalog-events",
            envelope = KafkaEventEnvelope(
                eventId = EVENT_ID,
                eventType = KafkaEventType.PRODUCT_DETAIL_VIEWED,
                aggregateId = PRODUCT_ID,
                payload = objectMapper.readTree("""{"productId":100}"""),
            ),
        )

        verify(productMetricRepository, never()).save(check<ProductMetric> { })
        verify(handledEventRepository, never()).save(check { })
    }

    @Test
    @DisplayName("payment succeeded event는 product item 수량을 누적한다")
    fun handleOrderEvent() {
        whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
        whenever(processedPaymentRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false)
        whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
        whenever(productMetricRepository.findByProductId(SECOND_PRODUCT_ID)).thenReturn(null)
        whenever(productMetricRepository.saveAll(check { })).thenAnswer { it.arguments[0] as List<ProductMetric> }

        handler.handle(
            topic = "order-events",
            envelope = KafkaEventEnvelope(
                eventId = EVENT_ID,
                eventType = KafkaEventType.PAYMENT_SUCCEEDED,
                aggregateId = ORDER_ID,
                payload = objectMapper.readTree(
                    """
	                    {
	                      "paymentId": 300,
	                      "orderId": 200,
	                      "userId": 10,
	                      "items": [
                        {"productId": 100, "quantity": 2},
                        {"productId": 101, "quantity": 3}
                      ]
                    }
                    """.trimIndent(),
                ),
            ),
        )

        verify(productMetricRepository).saveAll(
            check { metrics ->
                assertThat(metrics).hasSize(2)
                assertThat(metrics.map { it.productId }).containsExactlyInAnyOrder(PRODUCT_ID, SECOND_PRODUCT_ID)
                assertThat(metrics.first { it.productId == PRODUCT_ID }.unitsSold).isEqualTo(2)
                assertThat(metrics.first { it.productId == SECOND_PRODUCT_ID }.unitsSold).isEqualTo(3)
            },
        )
        verify(processedPaymentRepository).save(PAYMENT_ID)
        verify(handledEventRepository).save(check { assertThat(it.eventType).isEqualTo(KafkaEventType.PAYMENT_SUCCEEDED) })
    }

    @Test
    @DisplayName("같은 paymentId가 다른 eventId로 다시 와도 판매 수량은 한 번만 반영된다")
    fun skipDuplicatePaymentId() {
        whenever(handledEventRepository.existsByEventId(1L)).thenReturn(false)
        whenever(handledEventRepository.existsByEventId(2L)).thenReturn(false)
        whenever(processedPaymentRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false, true)
        whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
        whenever(productMetricRepository.saveAll(check { })).thenAnswer { it.arguments[0] as List<ProductMetric> }

        handler.handle(
            topic = "order-events",
            envelope = paymentSucceededEnvelope(eventId = 1L, paymentId = PAYMENT_ID, quantity = 2),
        )
        handler.handle(
            topic = "order-events",
            envelope = paymentSucceededEnvelope(eventId = 2L, paymentId = PAYMENT_ID, quantity = 2),
        )

        verify(productMetricRepository).saveAll(
            check { metrics ->
                assertThat(metrics).hasSize(1)
                assertThat(metrics.first().unitsSold).isEqualTo(2)
            },
        )
        verify(processedPaymentRepository).save(PAYMENT_ID)
        verify(handledEventRepository).save(check { handled -> assertThat(handled.eventId).isEqualTo(1L) })
        verify(handledEventRepository).save(check { handled -> assertThat(handled.eventId).isEqualTo(2L) })
    }

    private fun paymentSucceededEnvelope(
        eventId: Long,
        paymentId: Long,
        quantity: Int,
    ): KafkaEventEnvelope =
        KafkaEventEnvelope(
            eventId = eventId,
            eventType = KafkaEventType.PAYMENT_SUCCEEDED,
            aggregateId = ORDER_ID,
            payload = objectMapper.readTree(
                """
                {
                  "paymentId": $paymentId,
                  "orderId": $ORDER_ID,
                  "userId": 10,
                  "items": [
                    {"productId": $PRODUCT_ID, "quantity": $quantity}
                  ]
                }
                """.trimIndent(),
            ),
        )

    companion object {
        private const val EVENT_ID = 1L
        private const val ORDER_ID = 200L
        private const val PAYMENT_ID = 300L
        private const val PRODUCT_ID = 100L
        private const val SECOND_PRODUCT_ID = 101L
    }
}
