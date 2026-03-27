package com.loopers.application.metrics

import com.loopers.domain.idempotency.EventHandledRepository
import com.loopers.domain.metrics.ProductMetricsRepository
import com.loopers.testcontainers.KafkaTestContainersConfig
import com.loopers.testcontainers.MySqlTestContainersConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(MySqlTestContainersConfig::class, RedisTestContainersConfig::class, KafkaTestContainersConfig::class)
class MetricsEventProcessorTest @Autowired constructor(
    private val metricsEventProcessor: MetricsEventProcessor,
    private val productMetricsRepository: ProductMetricsRepository,
    private val eventHandledRepository: EventHandledRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("멱등성 테스트")
    @Nested
    inner class Idempotency {

        @DisplayName("같은 eventId로 2번 처리하면 두 번째는 skip된다")
        @Test
        fun duplicateEventIsSkipped() {
            // arrange
            val eventId = "test-event-id-001"
            val eventType = "LIKE_CREATED"
            val productId = 1L

            // act — 같은 이벤트 2번 처리
            metricsEventProcessor.processLikeCreated(eventId, eventType, productId)
            metricsEventProcessor.processLikeCreated(eventId, eventType, productId)

            // assert — likeCount는 1번만 증가
            val metrics = productMetricsRepository.findByProductId(productId)
            assertThat(metrics?.likeCount).isEqualTo(1)
        }

        @DisplayName("다른 eventId면 각각 처리된다")
        @Test
        fun differentEventsAreProcessed() {
            // arrange & act
            metricsEventProcessor.processLikeCreated("event-001", "LIKE_CREATED", 1L)
            metricsEventProcessor.processLikeCreated("event-002", "LIKE_CREATED", 1L)

            // assert
            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics?.likeCount).isEqualTo(2)
        }
    }

    @DisplayName("Metrics 업데이트")
    @Nested
    inner class MetricsUpdate {

        @DisplayName("좋아요 생성 시 likeCount가 증가한다")
        @Test
        fun likeCreatedIncrementsCount() {
            // act
            metricsEventProcessor.processLikeCreated("event-like-1", "LIKE_CREATED", 1L)

            // assert
            val metrics = productMetricsRepository.findByProductId(1L)
            assertAll(
                { assertThat(metrics).isNotNull },
                { assertThat(metrics?.likeCount).isEqualTo(1) },
                { assertThat(metrics?.lastEventAt).isNotNull() },
            )
        }

        @DisplayName("좋아요 취소 시 likeCount가 감소한다")
        @Test
        fun likeCancelledDecrementsCount() {
            // arrange
            metricsEventProcessor.processLikeCreated("event-like-1", "LIKE_CREATED", 1L)

            // act
            metricsEventProcessor.processLikeCancelled("event-unlike-1", "LIKE_CANCELLED", 1L)

            // assert
            val metrics = productMetricsRepository.findByProductId(1L)
            assertThat(metrics?.likeCount).isEqualTo(0)
        }

        @DisplayName("결제 승인 시 totalRevenue가 반영된다")
        @Test
        fun paymentApprovedUpdatesRevenue() {
            // act
            metricsEventProcessor.processPaymentApproved(
                eventId = "event-payment-1",
                eventType = "PAYMENT_APPROVED",
                productIds = listOf(1L, 2L),
                amount = 10000L,
            )

            // assert
            val metrics1 = productMetricsRepository.findByProductId(1L)
            val metrics2 = productMetricsRepository.findByProductId(2L)
            assertAll(
                { assertThat(metrics1?.totalRevenue).isEqualTo(5000L) },
                { assertThat(metrics2?.totalRevenue).isEqualTo(5000L) },
            )
        }
    }
}
