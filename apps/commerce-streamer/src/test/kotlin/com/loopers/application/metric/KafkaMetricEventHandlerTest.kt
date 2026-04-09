package com.loopers.application.metric

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.loopers.domain.metric.HandledEventRepository
import com.loopers.domain.metric.ProductLikeCountRepository
import com.loopers.domain.metric.ProductMetric
import com.loopers.domain.metric.ProductMetricRepository
import com.loopers.domain.metric.ProcessedPaymentRepository
import com.loopers.domain.ranking.ProductRankingRepository
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.infrastructure.outbox.KafkaEventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
    private val productRankingRepository: ProductRankingRepository = mock()
    private val handler = KafkaMetricEventHandler(
        productMetricRepository,
        handledEventRepository,
        productLikeCountRepository,
        processedPaymentRepository,
        productRankingRepository,
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

    @Nested
    @DisplayName("조회 이벤트 처리 시 ZSET에 +0.1 누적된다")
    inner class RankingViewEvent {

        @Test
        @DisplayName("PRODUCT_DETAIL_VIEWED → 해당 상품에 view 가중치(0.1) 반영")
        fun handle_viewEvent_incrementsRankingScore() {
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

            verify(productRankingRepository).incrementScore(PRODUCT_ID, RankingScorePolicy.VIEW_WEIGHT)
        }
    }

    @Nested
    @DisplayName("좋아요 등록 이벤트 처리 시 ZSET에 +0.2 누적된다")
    inner class RankingLikeEvent {

        @Test
        @DisplayName("PRODUCT_LIKE_REGISTERED → 해당 상품에 like 가중치(0.2) 반영")
        fun handle_likeRegistered_incrementsRankingScore() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productLikeCountRepository.countByProductId(PRODUCT_ID)).thenReturn(1)
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

            verify(productRankingRepository).incrementScore(PRODUCT_ID, RankingScorePolicy.LIKE_WEIGHT)
        }
    }

    @Nested
    @DisplayName("좋아요 취소 이벤트는 랭킹 점수에 반영하지 않는다")
    inner class RankingLikeCanceledEvent {

        @Test
        @DisplayName("PRODUCT_LIKE_CANCELED → 랭킹 점수 변경 없음 (product_metrics는 여전히 갱신)")
        fun handle_likeCanceled_doesNotIncrementRankingScore() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productLikeCountRepository.countByProductId(PRODUCT_ID)).thenReturn(0)
            whenever(productMetricRepository.save(check { })).thenAnswer { it.arguments[0] as ProductMetric }

            handler.handle(
                topic = "catalog-events",
                envelope = KafkaEventEnvelope(
                    eventId = EVENT_ID,
                    eventType = KafkaEventType.PRODUCT_LIKE_CANCELED,
                    aggregateId = PRODUCT_ID,
                    payload = objectMapper.readTree("""{"productId":100}"""),
                ),
            )

            verify(productRankingRepository, never()).incrementScore(any(), any())
        }
    }

    @Nested
    @DisplayName("결제 성공 이벤트 처리 시 상품별 ZSET에 +0.7 x quantity 누적된다")
    inner class RankingOrderEvent {

        @Test
        @DisplayName("PAYMENT_SUCCEEDED → 상품별로 order 가중치(0.7) × 수량 반영")
        fun handle_paymentSucceeded_incrementsRankingScorePerItem() {
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

            verify(productRankingRepository).incrementScore(PRODUCT_ID, RankingScorePolicy.ORDER_WEIGHT * 2)
            verify(productRankingRepository).incrementScore(SECOND_PRODUCT_ID, RankingScorePolicy.ORDER_WEIGHT * 3)
        }
    }

    @Nested
    @DisplayName("이미 처리한 이벤트는 랭킹에도 반영하지 않는다")
    inner class RankingIdempotency {

        @Test
        @DisplayName("이미 처리된 eventId → 랭킹 점수 변경 없음")
        fun handle_duplicateEvent_doesNotIncrementRankingScore() {
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

            verify(productRankingRepository, never()).incrementScore(any(), any())
        }

        @Test
        @DisplayName("이미 처리된 paymentId → 랭킹 점수 변경 없음")
        fun handle_duplicatePayment_doesNotIncrementRankingScore() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(processedPaymentRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(true)

            handler.handle(
                topic = "order-events",
                envelope = paymentSucceededEnvelope(eventId = EVENT_ID, paymentId = PAYMENT_ID, quantity = 2),
            )

            verify(productRankingRepository, never()).incrementScore(any(), any())
        }
    }

    @Nested
    @DisplayName("stale catalog event는 랭킹에 반영하지 않는다")
    inner class RankingStaleCatalogEvent {

        @Test
        @DisplayName("catalogEventVersion이 이미 더 높으면 랭킹 점수 변경 없음")
        fun handle_staleCatalogEvent_doesNotIncrementRankingScore() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            val existingMetric = ProductMetric.retrieve(
                productId = PRODUCT_ID,
                viewCount = 5,
                likeCount = 3,
                unitsSold = 0,
                catalogEventVersion = 100L,
                orderEventVersion = 0L,
            )
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(existingMetric)

            handler.handle(
                topic = "catalog-events",
                envelope = KafkaEventEnvelope(
                    eventId = 50L,
                    eventType = KafkaEventType.PRODUCT_DETAIL_VIEWED,
                    aggregateId = PRODUCT_ID,
                    payload = objectMapper.readTree("""{"productId":100}"""),
                ),
            )

            verify(productRankingRepository, never()).incrementScore(any(), any())
        }
    }

    @Nested
    @DisplayName("quantity가 0 이하인 아이템은 무시된다")
    inner class InvalidQuantity {

        @Test
        @DisplayName("quantity=0 → metric과 랭킹 모두 미반영")
        fun handle_zeroQuantity_skipsItem() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(processedPaymentRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false)

            handler.handle(
                topic = "order-events",
                envelope = KafkaEventEnvelope(
                    eventId = EVENT_ID,
                    eventType = KafkaEventType.PAYMENT_SUCCEEDED,
                    aggregateId = ORDER_ID,
                    payload = objectMapper.readTree(
                        """
                        {
                          "paymentId": $PAYMENT_ID,
                          "orderId": $ORDER_ID,
                          "userId": 10,
                          "items": [
                            {"productId": $PRODUCT_ID, "quantity": 0}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productMetricRepository, never()).saveAll(any<List<ProductMetric>>())
            verify(productRankingRepository, never()).incrementScore(any(), any())
        }

        @Test
        @DisplayName("quantity=-1 → metric과 랭킹 모두 미반영")
        fun handle_negativeQuantity_skipsItem() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(processedPaymentRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false)

            handler.handle(
                topic = "order-events",
                envelope = KafkaEventEnvelope(
                    eventId = EVENT_ID,
                    eventType = KafkaEventType.PAYMENT_SUCCEEDED,
                    aggregateId = ORDER_ID,
                    payload = objectMapper.readTree(
                        """
                        {
                          "paymentId": $PAYMENT_ID,
                          "orderId": $ORDER_ID,
                          "userId": 10,
                          "items": [
                            {"productId": $PRODUCT_ID, "quantity": -1}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productMetricRepository, never()).saveAll(any<List<ProductMetric>>())
            verify(productRankingRepository, never()).incrementScore(any(), any())
        }
    }

    @Nested
    @DisplayName("같은 productId가 중복으로 포함된 payment는 수량을 합산한다")
    inner class DuplicateProductId {

        @Test
        @DisplayName("같은 productId 2건 → 수량 합산하여 1건으로 처리")
        fun handle_duplicateProductId_aggregatesQuantity() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(processedPaymentRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
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
                          "paymentId": $PAYMENT_ID,
                          "orderId": $ORDER_ID,
                          "userId": 10,
                          "items": [
                            {"productId": $PRODUCT_ID, "quantity": 2},
                            {"productId": $PRODUCT_ID, "quantity": 3}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productMetricRepository).saveAll(
                check { metrics ->
                    assertThat(metrics).hasSize(1)
                    assertThat(metrics.first().unitsSold).isEqualTo(5)
                },
            )
            verify(productRankingRepository).incrementScore(PRODUCT_ID, RankingScorePolicy.ORDER_WEIGHT * 5)
        }
    }

    @Nested
    @DisplayName("Redis 장애 시 메인 파이프라인은 계속 동작한다")
    inner class RankingRedisFailure {

        @Test
        @DisplayName("incrementScore 예외 발생 → metrics 저장은 정상 완료")
        fun handle_redisFailure_metricsStillSaved() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productMetricRepository.save(check { })).thenAnswer { it.arguments[0] as ProductMetric }
            whenever(productRankingRepository.incrementScore(any(), any()))
                .thenThrow(RuntimeException("Redis connection refused"))

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
                },
            )
            verify(handledEventRepository).save(check { })
        }
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
