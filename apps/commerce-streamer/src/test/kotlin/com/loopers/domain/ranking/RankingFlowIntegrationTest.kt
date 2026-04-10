package com.loopers.domain.ranking

import com.loopers.config.kafka.event.CatalogEventType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap

/**
 * Fake Redis ZSET 기반으로 랭킹 파이프라인 전체 흐름을 검증한다.
 * Docker 없이 실행 가능하며, 실제 Redis ZSET 동작을 메모리에서 시뮬레이션한다.
 */
@DisplayName("랭킹 흐름 통합 테스트")
class RankingFlowIntegrationTest {

    private lateinit var fakeRepo: FakeRankingRepository
    private lateinit var scorePolicy: RankingScorePolicy

    @BeforeEach
    fun setUp() {
        fakeRepo = FakeRankingRepository()
        scorePolicy = RankingScorePolicy(viewWeight = 0.1, likeWeight = 0.2, orderWeight = 0.7)
    }

    private fun simulateEvent(
        productId: Long,
        eventType: CatalogEventType,
        delta: Long,
        occurredAt: ZonedDateTime,
    ) {
        val scoreIncrement = scorePolicy.calculateIncrement(eventType, delta)
        val key = RankingKeyGenerator.dailyKey(occurredAt.toLocalDate())
        fakeRepo.incrementScore(key, productId, scoreIncrement)
    }

    @DisplayName("체크리스트: 이벤트 발행 → ZSET 점수 반영 → 조회 E2E 흐름")
    @Nested
    inner class EventToZsetToApiFlow {

        @DisplayName("조회/좋아요/주문 이벤트가 각각 ZSET에 올바른 점수로 반영된다")
        @Test
        fun eventsAreReflectedInZset() {
            val date = ZonedDateTime.of(2026, 4, 10, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"))

            simulateEvent(101L, CatalogEventType.PRODUCT_VIEWED, 1, date)
            simulateEvent(101L, CatalogEventType.LIKE_CHANGED, 1, date)
            simulateEvent(101L, CatalogEventType.ORDER_COMPLETED, 1, date)

            val key = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 10))
            // 0.1 + 0.2 + 0.7 = 1.0
            assertThat(fakeRepo.score(key, 101L)).isCloseTo(1.0, Offset.offset(0.0001))
        }

        @DisplayName("ZSET 조회 시 점수 내림차순으로 정렬된다")
        @Test
        fun topNReturnsDescendingOrder() {
            val date = ZonedDateTime.of(2026, 4, 10, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"))

            simulateEvent(101L, CatalogEventType.ORDER_COMPLETED, 1, date) // 0.7
            repeat(5) { simulateEvent(202L, CatalogEventType.LIKE_CHANGED, 1, date) } // 1.0
            repeat(3) { simulateEvent(303L, CatalogEventType.PRODUCT_VIEWED, 1, date) } // 0.3

            val key = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 10))
            val topN = fakeRepo.topN(key, 10)

            assertThat(topN).hasSize(3)
            assertThat(topN[0].first).isEqualTo(202L) // 1.0
            assertThat(topN[1].first).isEqualTo(101L) // 0.7
            assertThat(topN[2].first).isEqualTo(303L) // 0.3
        }
    }

    @DisplayName("체크리스트: 일자가 변경되어도 이전 날짜의 랭킹 조회가 정상 동작한다")
    @Nested
    inner class DateChangeRetention {

        @DisplayName("어제와 오늘의 랭킹은 독립적으로 관리된다")
        @Test
        fun differentDatesAreIndependent() {
            val yesterday = ZonedDateTime.of(2026, 4, 9, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"))
            val today = ZonedDateTime.of(2026, 4, 10, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"))

            simulateEvent(101L, CatalogEventType.ORDER_COMPLETED, 2, yesterday)
            repeat(3) { simulateEvent(202L, CatalogEventType.LIKE_CHANGED, 1, today) }

            val yesterdayKey = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 9))
            val todayKey = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 10))

            assertThat(fakeRepo.topN(yesterdayKey, 10)).hasSize(1)
            assertThat(fakeRepo.topN(yesterdayKey, 10)[0].first).isEqualTo(101L)
            assertThat(fakeRepo.topN(todayKey, 10)).hasSize(1)
            assertThat(fakeRepo.topN(todayKey, 10)[0].first).isEqualTo(202L)
        }

        @DisplayName("오늘 이벤트가 발생해도 어제 랭킹은 변하지 않는다")
        @Test
        fun todayEventsDoNotAffectYesterday() {
            val yesterday = ZonedDateTime.of(2026, 4, 9, 23, 0, 0, 0, ZoneId.of("Asia/Seoul"))
            val today = ZonedDateTime.of(2026, 4, 10, 1, 0, 0, 0, ZoneId.of("Asia/Seoul"))

            simulateEvent(101L, CatalogEventType.LIKE_CHANGED, 1, yesterday)
            val yesterdayKey = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 9))
            val scoreBefore = fakeRepo.score(yesterdayKey, 101L)

            simulateEvent(101L, CatalogEventType.ORDER_COMPLETED, 1, today)
            assertThat(fakeRepo.score(yesterdayKey, 101L)).isEqualTo(scoreBefore)
        }
    }

    @DisplayName("체크리스트: 가중치가 의도대로 랭킹 순서에 반영된다 (주문 1건 > 좋아요 3건)")
    @Nested
    inner class WeightOrdering {

        @DisplayName("주문 1건(0.7)이 좋아요 3건(0.6)보다 높은 순위를 갖는다")
        @Test
        fun singleOrderOutranksThreeLikes() {
            val date = ZonedDateTime.of(2026, 4, 10, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"))

            simulateEvent(100L, CatalogEventType.ORDER_COMPLETED, 1, date)
            repeat(3) { simulateEvent(200L, CatalogEventType.LIKE_CHANGED, 1, date) }

            val key = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 10))
            val topN = fakeRepo.topN(key, 10)

            assertThat(topN[0].first).isEqualTo(100L)
            assertThat(topN[1].first).isEqualTo(200L)
            assertThat(topN[0].second).isGreaterThan(topN[1].second)
        }

        @DisplayName("좋아요 취소는 점수를 감소시킨다")
        @Test
        fun unlikeDecreasesScore() {
            val date = ZonedDateTime.of(2026, 4, 10, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"))

            repeat(3) { simulateEvent(100L, CatalogEventType.LIKE_CHANGED, 1, date) }
            simulateEvent(100L, CatalogEventType.LIKE_CHANGED, -1, date)

            val key = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 10))
            // 좋아요 3건(0.6) - 취소 1건(0.2) = 0.4
            assertThat(fakeRepo.score(key, 100L)).isCloseTo(0.4, Offset.offset(0.0001))
        }

        @DisplayName("복합 이벤트 시 가중치가 올바르게 합산된다")
        @Test
        fun compositeEventsAreWeightedCorrectly() {
            val date = ZonedDateTime.of(2026, 4, 10, 12, 0, 0, 0, ZoneId.of("Asia/Seoul"))

            // 상품A: 조회 10건(1.0) + 좋아요 5건(1.0) + 주문 2건(1.4) = 3.4
            repeat(10) { simulateEvent(100L, CatalogEventType.PRODUCT_VIEWED, 1, date) }
            repeat(5) { simulateEvent(100L, CatalogEventType.LIKE_CHANGED, 1, date) }
            simulateEvent(100L, CatalogEventType.ORDER_COMPLETED, 2, date)

            // 상품B: 조회 50건(5.0)
            repeat(50) { simulateEvent(200L, CatalogEventType.PRODUCT_VIEWED, 1, date) }

            val key = RankingKeyGenerator.dailyKey(LocalDate.of(2026, 4, 10))
            val topN = fakeRepo.topN(key, 10)

            assertThat(topN[0].first).isEqualTo(200L)
            assertThat(topN[0].second).isCloseTo(5.0, Offset.offset(0.0001))
            assertThat(topN[1].first).isEqualTo(100L)
            assertThat(topN[1].second).isCloseTo(3.4, Offset.offset(0.0001))
        }
    }

    /**
     * Redis ZSET 동작을 시뮬레이션하는 Fake Repository.
     */
    private class FakeRankingRepository : RankingRepository {
        private val store = ConcurrentHashMap<String, ConcurrentHashMap<Long, Double>>()

        override fun incrementScore(key: String, productId: Long, score: Double) {
            store.computeIfAbsent(key) { ConcurrentHashMap() }
                .merge(productId, score, Double::plus)
        }

        /** score 내림차순으로 상위 N개 반환. Pair<productId, score> */
        fun topN(key: String, count: Int): List<Pair<Long, Double>> {
            val members = store[key] ?: return emptyList()
            return members.entries
                .sortedByDescending { it.value }
                .take(count)
                .map { it.key to it.value }
        }

        fun score(key: String, productId: Long): Double? {
            return store[key]?.get(productId)
        }
    }
}
