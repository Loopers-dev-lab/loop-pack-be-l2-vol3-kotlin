package com.loopers.domain.ranking

import com.loopers.domain.metrics.OrderItemMetrics
import com.loopers.infrastructure.ranking.RankingEventJpaRepository
import com.loopers.infrastructure.ranking.RankingMetricJpaRepository
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
class RankingEventServiceIntegrationTest @Autowired constructor(
    private val rankingEventService: RankingEventService,
    private val rankingEventJpaRepository: RankingEventJpaRepository,
    private val rankingMetricJpaRepository: RankingMetricJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("조회 배치 이벤트 저장")
    @Nested
    inner class SaveViewBatch {
        @DisplayName("배치 조회 이벤트가 상품별로 저장된다.")
        @Test
        fun savesViewBatchEvents() {
            // arrange
            val views = listOf(
                ViewCount(productId = 1L, count = 150),
                ViewCount(productId = 2L, count = 80),
            )

            // act
            rankingEventService.saveViewBatch(views, "batch-uuid-1")

            // assert
            val events = rankingEventJpaRepository.findAll()
            assertAll(
                { assertThat(events).hasSize(2) },
                { assertThat(events[0].eventType).isEqualTo(RankingEventType.VIEW) },
                { assertThat(events[0].score).isEqualTo(0.1 * 150) },
                { assertThat(events[0].rawCount).isEqualTo(150) },
                { assertThat(events[1].score).isEqualTo(0.1 * 80) },
            )
        }
    }

    @DisplayName("좋아요 이벤트 저장")
    @Nested
    inner class SaveLikeEvent {
        @DisplayName("좋아요 이벤트가 가중치 적용되어 저장된다.")
        @Test
        fun savesLikeEvent() {
            // act
            rankingEventService.saveLikeEvent(productId = 10L, eventId = 1L)

            // assert
            val events = rankingEventJpaRepository.findAll()
            assertAll(
                { assertThat(events).hasSize(1) },
                { assertThat(events[0].productId).isEqualTo(10L) },
                { assertThat(events[0].eventType).isEqualTo(RankingEventType.LIKE) },
                { assertThat(events[0].score).isEqualTo(0.2) },
            )
        }

        @DisplayName("동일한 이벤트 ID로 두 번 호출하면 한 번만 저장된다.")
        @Test
        fun idempotent() {
            // act
            rankingEventService.saveLikeEvent(productId = 10L, eventId = 1L)
            rankingEventService.saveLikeEvent(productId = 10L, eventId = 1L)

            // assert
            val events = rankingEventJpaRepository.findAll()
            assertThat(events).hasSize(1)
        }
    }

    @DisplayName("주문 이벤트 저장")
    @Nested
    inner class SaveOrderEvent {
        @DisplayName("주문 아이템별로 가중치 적용되어 저장된다.")
        @Test
        fun savesOrderEvents() {
            // arrange
            val items = listOf(
                OrderItemMetrics(productId = 1L, productPrice = 10000, quantity = 2),
                OrderItemMetrics(productId = 2L, productPrice = 5000, quantity = 1),
            )

            // act
            rankingEventService.saveOrderEvent(items, eventId = 1L)

            // assert
            val events = rankingEventJpaRepository.findAll()
            assertAll(
                { assertThat(events).hasSize(2) },
                { assertThat(events[0].eventType).isEqualTo(RankingEventType.ORDER) },
                { assertThat(events[0].score).isEqualTo(0.6 * 10000 * 2) },
                { assertThat(events[1].score).isEqualTo(0.6 * 5000 * 1) },
            )
        }
    }
}
