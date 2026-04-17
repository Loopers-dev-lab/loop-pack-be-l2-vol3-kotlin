package com.loopers.application.metric

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.loopers.domain.metric.HandledEventRepository
import com.loopers.domain.metric.ProductLikeCountRepository
import com.loopers.domain.metric.ProductMetric
import com.loopers.domain.metric.ProductMetricDaily
import com.loopers.domain.metric.ProductMetricDailyRepository
import com.loopers.domain.metric.ProductMetricRepository
import com.loopers.domain.metric.ProcessedPaymentRepository
import com.loopers.domain.ranking.ProductRankingRepository
import com.loopers.domain.ranking.RankingScorePolicy
import com.loopers.infrastructure.outbox.KafkaEventType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@DisplayName("KafkaMetricEventHandler")
@ExtendWith(OutputCaptureExtension::class)
class KafkaMetricEventHandlerTest {
    private val objectMapper = jacksonObjectMapper()
    private val productMetricRepository: ProductMetricRepository = mock()
    private val productMetricDailyRepository: ProductMetricDailyRepository = mock()
    private val handledEventRepository: HandledEventRepository = mock()
    private val productLikeCountRepository: ProductLikeCountRepository = mock()
    private val processedPaymentRepository: ProcessedPaymentRepository = mock()
    private val productRankingRepository: ProductRankingRepository = mock()
    private val handler = KafkaMetricEventHandler(
        productMetricRepository,
        productMetricDailyRepository,
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
        whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

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
        verify(productMetricDailyRepository).save(
            check { daily ->
                assertThat(daily.productId).isEqualTo(PRODUCT_ID)
                assertThat(daily.viewCount).isEqualTo(1)
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
        whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

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
        verify(productMetricDailyRepository).save(
            check { daily ->
                assertThat(daily.productId).isEqualTo(PRODUCT_ID)
                assertThat(daily.likeCount).isEqualTo(1)
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
        verify(productMetricDailyRepository, never()).save(any())
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
        whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

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
                        {"productId": 100, "quantity": 2, "sellingPrice": 10000},
                        {"productId": 101, "quantity": 3, "sellingPrice": 5000}
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
        verify(productMetricDailyRepository, times(2)).save(any())
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
        whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

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
        // daily도 첫 이벤트에서만 저장, 두 번째 이벤트는 paymentId 중복이라 skip
        verify(productMetricDailyRepository, times(1)).save(any())
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
            whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

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
            whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

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
    @DisplayName("좋아요 취소 이벤트는 랭킹 점수·daily에 반영하지 않는다")
    inner class RankingLikeCanceledEvent {

        @Test
        @DisplayName("PRODUCT_LIKE_CANCELED → 랭킹 점수 변경 없음, daily 저장 없음 (product_metrics는 여전히 갱신)")
        fun handle_likeCanceled_doesNotIncrementRankingScoreOrDaily() {
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
            verify(productMetricDailyRepository, never()).save(any())
        }
    }

    @Nested
    @DisplayName("결제 성공 이벤트 처리 시 상품별 ZSET에 ORDER_WEIGHT × ln(sellingPrice × quantity) 누적된다")
    inner class RankingOrderEvent {

        @Test
        @DisplayName("PAYMENT_SUCCEEDED → 상품별로 price 기반 score 반영")
        fun handle_paymentSucceeded_incrementsRankingScorePerItem() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(processedPaymentRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productMetricRepository.findByProductId(SECOND_PRODUCT_ID)).thenReturn(null)
            whenever(productMetricRepository.saveAll(check { })).thenAnswer { it.arguments[0] as List<ProductMetric> }
            whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

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
                            {"productId": 100, "quantity": 2, "sellingPrice": 10000},
                            {"productId": 101, "quantity": 3, "sellingPrice": 5000}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            val productIdCaptor = org.mockito.kotlin.argumentCaptor<Long>()
            val incrementCaptor = org.mockito.kotlin.argumentCaptor<Double>()
            verify(productRankingRepository, times(2))
                .incrementScore(productIdCaptor.capture(), incrementCaptor.capture())

            val expected100 = RankingScorePolicy.ORDER_WEIGHT * kotlin.math.ln(10000.0 * 2)
            val expected101 = RankingScorePolicy.ORDER_WEIGHT * kotlin.math.ln(5000.0 * 3)
            val captured = productIdCaptor.allValues.zip(incrementCaptor.allValues).toMap()
            assertThat(captured[PRODUCT_ID]).isCloseTo(expected100, org.assertj.core.data.Offset.offset(0.001))
            assertThat(captured[SECOND_PRODUCT_ID]).isCloseTo(expected101, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        @DisplayName("sellingPrice 필드가 없으면 해당 아이템은 랭킹에 반영하지 않는다 (totalAmount=0 → score=0)")
        fun handle_missingPrice_skipsRanking() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(processedPaymentRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productMetricRepository.saveAll(check { })).thenAnswer { it.arguments[0] as List<ProductMetric> }
            whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

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
                            {"productId": $PRODUCT_ID, "quantity": 1}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productRankingRepository, never()).incrementScore(any(), any())
        }
    }

    @Nested
    @DisplayName("이미 처리한 이벤트는 랭킹·daily에도 반영하지 않는다")
    inner class RankingIdempotency {

        @Test
        @DisplayName("이미 처리된 eventId → 랭킹 점수·daily 변경 없음")
        fun handle_duplicateEvent_doesNotIncrementRankingScoreOrDaily() {
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
            verify(productMetricDailyRepository, never()).save(any())
        }

        @Test
        @DisplayName("이미 처리된 paymentId → 랭킹 점수·daily 변경 없음")
        fun handle_duplicatePayment_doesNotIncrementRankingScoreOrDaily() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(processedPaymentRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(true)

            handler.handle(
                topic = "order-events",
                envelope = paymentSucceededEnvelope(eventId = EVENT_ID, paymentId = PAYMENT_ID, quantity = 2),
            )

            verify(productRankingRepository, never()).incrementScore(any(), any())
            verify(productMetricDailyRepository, never()).save(any())
        }
    }

    @Nested
    @DisplayName("stale catalog event는 랭킹·daily에 반영하지 않는다")
    inner class RankingStaleCatalogEvent {

        @Test
        @DisplayName("catalogEventVersion이 이미 더 높으면 랭킹 점수·daily 변경 없음")
        fun handle_staleCatalogEvent_doesNotIncrementRankingScoreOrDaily() {
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
            verify(productMetricDailyRepository, never()).save(any())
        }
    }

    @Nested
    @DisplayName("quantity가 0 이하인 아이템 처리")
    inner class InvalidQuantity {

        @Test
        @DisplayName("quantity=0 (음수 아님) → per-item skip, daily 저장 없음, 이벤트 내 정상 아이템만 반영 안 됨")
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
            verify(productMetricDailyRepository, never()).save(any())
            verify(productRankingRepository, never()).incrementScore(any(), any())
        }

        @Test
        @DisplayName("quantity=-1 (음수) → 정책 C: 이벤트 전체 skip (metric·daily·ranking 모두 미반영)")
        fun handle_negativeQuantity_skipsEntireEvent() {
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
                            {"productId": $PRODUCT_ID, "quantity": -1, "sellingPrice": 10000}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productMetricRepository, never()).saveAll(any<List<ProductMetric>>())
            verify(productMetricDailyRepository, never()).save(any())
            verify(productRankingRepository, never()).incrementScore(any(), any())
        }

        @Test
        @DisplayName("quantity가 비정수 문자열이면 per-item skip + WARN 로그 (정책 C event-skip이 아닌 per-item)")
        fun handle_nonIntegerQuantity_skipsItemWithWarn(output: CapturedOutput) {
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
                            {"productId": $PRODUCT_ID, "quantity": "invalid", "sellingPrice": 10000}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productMetricRepository, never()).saveAll(any<List<ProductMetric>>())
            verify(productMetricDailyRepository, never()).save(any())
            verify(productRankingRepository, never()).incrementScore(any(), any())
            assertThat(output.all)
                .contains("Skip item with non-integer quantity")
                .contains("eventId=$EVENT_ID")
                .contains("productId=$PRODUCT_ID")
        }
    }

    @Nested
    @DisplayName("정책 C: 음수·필수 필드 누락 PAYMENT_SUCCEEDED 이벤트는 전체 skip된다")
    inner class InvalidItemFieldPolicyC {

        @Test
        @DisplayName("quantity 필드 누락 → 이벤트 전체 skip (per-item skip으로 영구 유실 방지)")
        fun handle_missingQuantity_skipsEntireEvent() {
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
                            {"productId": $PRODUCT_ID, "sellingPrice": 10000},
                            {"productId": $SECOND_PRODUCT_ID, "quantity": 2, "sellingPrice": 5000}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productMetricRepository, never()).saveAll(any<List<ProductMetric>>())
            verify(productMetricDailyRepository, never()).save(any())
            verify(productRankingRepository, never()).incrementScore(any(), any())
        }

        @Test
        @DisplayName("productId 필드 누락 → 이벤트 전체 skip")
        fun handle_missingProductId_skipsEntireEvent() {
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
                            {"quantity": 2, "sellingPrice": 10000},
                            {"productId": $SECOND_PRODUCT_ID, "quantity": 3, "sellingPrice": 5000}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productMetricRepository, never()).saveAll(any<List<ProductMetric>>())
            verify(productMetricDailyRepository, never()).save(any())
            verify(productRankingRepository, never()).incrementScore(any(), any())
        }

        @Test
        @DisplayName("sellingPrice < 0 → 이벤트 전체 skip")
        fun handle_negativeSellingPrice_skipsEntireEvent() {
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
                            {"productId": $PRODUCT_ID, "quantity": 2, "sellingPrice": -1000}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productMetricRepository, never()).saveAll(any<List<ProductMetric>>())
            verify(productMetricDailyRepository, never()).save(any())
            verify(productRankingRepository, never()).incrementScore(any(), any())
        }

        @Test
        @DisplayName("정상 아이템 1개 + 음수 아이템 1개 섞임 → 이벤트 전체 skip (정책 B 아닌 C 검증)")
        fun handle_oneNegativeAmongValid_skipsEntireEvent() {
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
                            {"productId": $PRODUCT_ID, "quantity": 2, "sellingPrice": 10000},
                            {"productId": $SECOND_PRODUCT_ID, "quantity": -1, "sellingPrice": 5000}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productMetricRepository, never()).saveAll(any<List<ProductMetric>>())
            verify(productMetricDailyRepository, never()).save(any())
            verify(productRankingRepository, never()).incrementScore(any(), any())
        }

        @Test
        @DisplayName("정책 C 트리거 시 WARN 로그로 eventId·paymentId가 기록된다 (운영 알림 계약)")
        fun handle_policyC_emitsWarnLog(output: CapturedOutput) {
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
                            {"productId": $PRODUCT_ID, "quantity": 2, "sellingPrice": -1000}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            assertThat(output.all)
                .contains("Skipping PAYMENT_SUCCEEDED with invalid item fields")
                .contains("eventId=$EVENT_ID")
                .contains("paymentId=$PAYMENT_ID")
        }
    }

    @Nested
    @DisplayName("items 누락/빈 배열/null 케이스")
    inner class EmptyItems {

        @Test
        @DisplayName("items가 JSON null이면 daily 저장 없음, saveAll 없음")
        fun handle_itemsNull_daily_not_saved() {
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
                          "items": null
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productMetricRepository, never()).saveAll(any<List<ProductMetric>>())
            verify(productMetricDailyRepository, never()).save(any())
            verify(productRankingRepository, never()).incrementScore(any(), any())
        }

        @Test
        @DisplayName("items 배열이 비어 있으면 daily 저장 없음, saveAll 없음")
        fun handle_emptyItems_daily_not_saved() {
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
                          "items": []
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            verify(productMetricRepository, never()).saveAll(any<List<ProductMetric>>())
            verify(productMetricDailyRepository, never()).save(any())
            verify(productRankingRepository, never()).incrementScore(any(), any())
        }
    }

    @Nested
    @DisplayName("같은 productId가 중복으로 포함된 payment는 수량을 합산한다")
    inner class DuplicateProductId {

        @Test
        @DisplayName("같은 productId 2건 → 수량 합산, daily도 합산된 값으로 1회 저장")
        fun handle_duplicateProductId_aggregatesQuantity() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(processedPaymentRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productMetricRepository.saveAll(check { })).thenAnswer { it.arguments[0] as List<ProductMetric> }
            whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

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
                            {"productId": $PRODUCT_ID, "quantity": 2, "sellingPrice": 10000},
                            {"productId": $PRODUCT_ID, "quantity": 3, "sellingPrice": 10000}
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
            verify(productMetricDailyRepository).save(
                check { daily ->
                    assertThat(daily.productId).isEqualTo(PRODUCT_ID)
                    assertThat(daily.unitsSold).isEqualTo(5)
                    assertThat(daily.salesAmount).isEqualTo(50_000L)
                },
            )
            val expectedScore = RankingScorePolicy.ORDER_WEIGHT * kotlin.math.ln(50000.0)
            val captor = org.mockito.kotlin.argumentCaptor<Double>()
            verify(productRankingRepository).incrementScore(org.mockito.kotlin.eq(PRODUCT_ID), captor.capture())
            assertThat(captor.firstValue).isCloseTo(expectedScore, org.assertj.core.data.Offset.offset(0.001))
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
            whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }
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

    @Nested
    @DisplayName("daily 적재: 이벤트별 대상 필드가 정확히 누적된다")
    inner class DailyPersistence {

        @Test
        @DisplayName("PRODUCT_DETAIL_VIEWED → daily.viewCount=1, metricDate=오늘(서울)")
        fun handle_detailViewed_savesDaily() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productMetricRepository.save(check { })).thenAnswer { it.arguments[0] as ProductMetric }
            whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

            val today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"))
            handler.handle(
                topic = "catalog-events",
                envelope = KafkaEventEnvelope(
                    eventId = EVENT_ID,
                    eventType = KafkaEventType.PRODUCT_DETAIL_VIEWED,
                    aggregateId = PRODUCT_ID,
                    payload = objectMapper.readTree("""{"productId":100}"""),
                ),
            )

            verify(productMetricDailyRepository).save(
                check { daily ->
                    assertThat(daily.productId).isEqualTo(PRODUCT_ID)
                    assertThat(daily.metricDate).isEqualTo(today)
                    assertThat(daily.viewCount).isEqualTo(1)
                    assertThat(daily.likeCount).isEqualTo(0)
                    assertThat(daily.unitsSold).isEqualTo(0)
                    assertThat(daily.salesAmount).isEqualTo(0L)
                    assertThat(daily.orderScore).isEqualTo(0.0)
                },
            )
        }

        @Test
        @DisplayName("PRODUCT_LIKE_REGISTERED → daily.likeCount=1")
        fun handle_likeRegistered_savesDaily() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productLikeCountRepository.countByProductId(PRODUCT_ID)).thenReturn(1)
            whenever(productMetricRepository.save(check { })).thenAnswer { it.arguments[0] as ProductMetric }
            whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

            handler.handle(
                topic = "catalog-events",
                envelope = KafkaEventEnvelope(
                    eventId = EVENT_ID,
                    eventType = KafkaEventType.PRODUCT_LIKE_REGISTERED,
                    aggregateId = PRODUCT_ID,
                    payload = objectMapper.readTree("""{"productId":100}"""),
                ),
            )

            verify(productMetricDailyRepository).save(
                check { daily ->
                    assertThat(daily.productId).isEqualTo(PRODUCT_ID)
                    assertThat(daily.likeCount).isEqualTo(1)
                    assertThat(daily.viewCount).isEqualTo(0)
                },
            )
        }

        @Test
        @DisplayName("PAYMENT_SUCCEEDED → 상품별 daily.unitsSold/salesAmount/orderScore 누적")
        fun handle_paymentSucceeded_savesDailyPerProduct() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(processedPaymentRepository.existsByPaymentId(PAYMENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productMetricRepository.findByProductId(SECOND_PRODUCT_ID)).thenReturn(null)
            whenever(productMetricRepository.saveAll(check { })).thenAnswer { it.arguments[0] as List<ProductMetric> }
            whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

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
                            {"productId": $PRODUCT_ID, "quantity": 2, "sellingPrice": 10000},
                            {"productId": $SECOND_PRODUCT_ID, "quantity": 3, "sellingPrice": 5000}
                          ]
                        }
                        """.trimIndent(),
                    ),
                ),
            )

            val dailyCaptor = org.mockito.kotlin.argumentCaptor<ProductMetricDaily>()
            verify(productMetricDailyRepository, times(2)).save(dailyCaptor.capture())

            val byProduct = dailyCaptor.allValues.associateBy { it.productId }
            val firstDaily = byProduct[PRODUCT_ID]!!
            val secondDaily = byProduct[SECOND_PRODUCT_ID]!!

            assertThat(firstDaily.unitsSold).isEqualTo(2)
            assertThat(firstDaily.salesAmount).isEqualTo(20_000L)
            val expected100 = RankingScorePolicy.ORDER_WEIGHT * kotlin.math.ln(20_000.0)
            assertThat(firstDaily.orderScore).isCloseTo(expected100, org.assertj.core.data.Offset.offset(0.001))

            assertThat(secondDaily.unitsSold).isEqualTo(3)
            assertThat(secondDaily.salesAmount).isEqualTo(15_000L)
            val expected101 = RankingScorePolicy.ORDER_WEIGHT * kotlin.math.ln(15_000.0)
            assertThat(secondDaily.orderScore).isCloseTo(expected101, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        @DisplayName("daily.save 실패 시 예외를 catch하지 않고 그대로 전파한다 (트랜잭션 롤백 위임)")
        fun handle_dailySaveFailure_propagatesException() {
            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productMetricRepository.save(check { })).thenAnswer { it.arguments[0] as ProductMetric }
            whenever(productMetricDailyRepository.save(any()))
                .thenThrow(RuntimeException("Daily DB write failed"))

            assertThatThrownBy {
                handler.handle(
                    topic = "catalog-events",
                    envelope = KafkaEventEnvelope(
                        eventId = EVENT_ID,
                        eventType = KafkaEventType.PRODUCT_DETAIL_VIEWED,
                        aggregateId = PRODUCT_ID,
                        payload = objectMapper.readTree("""{"productId":100}"""),
                    ),
                )
            }.isInstanceOf(RuntimeException::class.java)
                .hasMessageContaining("Daily DB write failed")
        }
    }

    @Nested
    @DisplayName("자정 경계: metricDate는 주입된 Clock(서울 시간대) 기준으로 결정된다")
    inner class MidnightBoundary {

        private val seoulZone = ZoneId.of("Asia/Seoul")

        @Test
        @DisplayName("KST 23:59:59.999 (UTC 14:59:59.999) → metricDate = 당일")
        fun handle_justBeforeMidnight_usesCurrentDay() {
            // UTC 2026-04-16T14:59:59.999Z = KST 2026-04-16T23:59:59.999
            val fixedClock = Clock.fixed(Instant.parse("2026-04-16T14:59:59.999Z"), seoulZone)
            val handlerWithClock = KafkaMetricEventHandler(
                productMetricRepository,
                productMetricDailyRepository,
                handledEventRepository,
                productLikeCountRepository,
                processedPaymentRepository,
                productRankingRepository,
                fixedClock,
            )

            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productMetricRepository.save(check { })).thenAnswer { it.arguments[0] as ProductMetric }
            whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

            handlerWithClock.handle(
                topic = "catalog-events",
                envelope = KafkaEventEnvelope(
                    eventId = EVENT_ID,
                    eventType = KafkaEventType.PRODUCT_DETAIL_VIEWED,
                    aggregateId = PRODUCT_ID,
                    payload = objectMapper.readTree("""{"productId":100}"""),
                ),
            )

            verify(productMetricDailyRepository).save(
                check { daily ->
                    assertThat(daily.metricDate).isEqualTo(LocalDate.of(2026, 4, 16))
                },
            )
        }

        @Test
        @DisplayName("KST 00:00:00 (UTC 15:00:00) → metricDate = 다음 날")
        fun handle_atMidnight_usesNextDay() {
            // UTC 2026-04-16T15:00:00Z = KST 2026-04-17T00:00:00
            val fixedClock = Clock.fixed(Instant.parse("2026-04-16T15:00:00Z"), seoulZone)
            val handlerWithClock = KafkaMetricEventHandler(
                productMetricRepository,
                productMetricDailyRepository,
                handledEventRepository,
                productLikeCountRepository,
                processedPaymentRepository,
                productRankingRepository,
                fixedClock,
            )

            whenever(handledEventRepository.existsByEventId(EVENT_ID)).thenReturn(false)
            whenever(productMetricRepository.findByProductId(PRODUCT_ID)).thenReturn(null)
            whenever(productMetricRepository.save(check { })).thenAnswer { it.arguments[0] as ProductMetric }
            whenever(productMetricDailyRepository.save(any())).thenAnswer { it.arguments[0] as ProductMetricDaily }

            handlerWithClock.handle(
                topic = "catalog-events",
                envelope = KafkaEventEnvelope(
                    eventId = EVENT_ID,
                    eventType = KafkaEventType.PRODUCT_DETAIL_VIEWED,
                    aggregateId = PRODUCT_ID,
                    payload = objectMapper.readTree("""{"productId":100}"""),
                ),
            )

            verify(productMetricDailyRepository).save(
                check { daily ->
                    assertThat(daily.metricDate).isEqualTo(LocalDate.of(2026, 4, 17))
                },
            )
        }
    }

    private fun paymentSucceededEnvelope(
        eventId: Long,
        paymentId: Long,
        quantity: Int,
        sellingPrice: Long = 10000,
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
                    {"productId": $PRODUCT_ID, "quantity": $quantity, "sellingPrice": $sellingPrice}
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
