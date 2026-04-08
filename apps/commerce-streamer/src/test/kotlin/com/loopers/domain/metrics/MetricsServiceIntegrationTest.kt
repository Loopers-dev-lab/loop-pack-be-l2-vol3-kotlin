package com.loopers.domain.metrics

import com.loopers.infrastructure.metrics.EventHandledJpaRepository
import com.loopers.infrastructure.metrics.ProductMetricsJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class MetricsServiceIntegrationTest @Autowired constructor(
    private val metricsService: MetricsService,
    private val productMetricsJpaRepository: ProductMetricsJpaRepository,
    private val eventHandledJpaRepository: EventHandledJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("좋아요 카운트 집계")
    @Nested
    inner class LikeCountMetrics {
        @DisplayName("좋아요 이벤트를 처리하면, product_metrics의 likeCount가 증가한다.")
        @Test
        fun incrementsLikeCount_whenLikeEventProcessed() {
            // act
            metricsService.incrementLikeCount(productId = 1L, eventId = 100L)

            // assert
            val metrics = productMetricsJpaRepository.findByProductId(1L)
            assertThat(metrics).isNotNull
            assertThat(metrics!!.likeCount).isEqualTo(1)
        }

        @DisplayName("좋아요 취소 이벤트를 처리하면, product_metrics의 likeCount가 감소한다.")
        @Test
        fun decrementsLikeCount_whenUnlikeEventProcessed() {
            // arrange
            metricsService.incrementLikeCount(productId = 1L, eventId = 100L)

            // act
            metricsService.decrementLikeCount(productId = 1L, eventId = 101L)

            // assert
            val metrics = productMetricsJpaRepository.findByProductId(1L)
            assertThat(metrics!!.likeCount).isEqualTo(0)
        }

        @DisplayName("동일한 이벤트 ID로 두 번 처리하면, 한 번만 반영된다 (멱등성).")
        @Test
        fun processesOnlyOnce_whenSameEventIdProcessedTwice() {
            // act
            metricsService.incrementLikeCount(productId = 1L, eventId = 100L)
            metricsService.incrementLikeCount(productId = 1L, eventId = 100L)

            // assert
            val metrics = productMetricsJpaRepository.findByProductId(1L)
            assertThat(metrics!!.likeCount).isEqualTo(1)
        }
    }

    @DisplayName("주문 집계")
    @Nested
    inner class OrderMetrics {
        @DisplayName("주문 이벤트를 처리하면, 관련 상품의 orderCount가 수량만큼 증가한다.")
        @Test
        fun incrementsOrderCount_byQuantity_whenOrderEventProcessed() {
            // arrange
            val items = listOf(
                OrderItemMetrics(productId = 1L, productPrice = 10000, quantity = 3),
                OrderItemMetrics(productId = 2L, productPrice = 20000, quantity = 1),
            )

            // act
            metricsService.recordOrder(items = items, eventId = 200L)

            // assert
            val metrics1 = productMetricsJpaRepository.findByProductId(1L)
            val metrics2 = productMetricsJpaRepository.findByProductId(2L)
            assertAll(
                { assertThat(metrics1!!.orderCount).isEqualTo(3) },
                { assertThat(metrics2!!.orderCount).isEqualTo(1) },
            )
        }

        @DisplayName("동일한 주문 이벤트 ID로 두 번 처리하면, 한 번만 반영된다.")
        @Test
        fun processesOnlyOnce_whenSameOrderEventProcessedTwice() {
            // arrange
            val items = listOf(OrderItemMetrics(productId = 1L, productPrice = 10000, quantity = 2))

            // act
            metricsService.recordOrder(items = items, eventId = 200L)
            metricsService.recordOrder(items = items, eventId = 200L)

            // assert
            val metrics = productMetricsJpaRepository.findByProductId(1L)
            assertThat(metrics!!.orderCount).isEqualTo(2)
        }
    }
}
