package com.loopers.domain.ranking

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("RankingService")
class RankingServiceTest {

    private val rankingRepository: RankingRepository = mockk()
    private val periodicRankingRepository: PeriodicRankingRepository = mockk()
    private val rankingService = RankingService(rankingRepository, periodicRankingRepository)

    @DisplayName("DAILY 랭킹 (Redis ZSET 경로)")
    @Nested
    inner class DailyRankings {
        private val date = LocalDate.of(2026, 4, 10)
        private val key = "ranking:all:20260410"

        @DisplayName("ZSET 에서 score 내림차순으로 상품을 조회하고 1-based 순위를 부여한다")
        @Test
        fun returnsTopRankingsWithOneBasedRank() {
            every { rankingRepository.getTopN(key, 0, 3) } returns listOf(
                RankingEntry(productId = 101L, score = 5.0),
                RankingEntry(productId = 202L, score = 3.5),
                RankingEntry(productId = 303L, score = 1.2),
            )
            every { rankingRepository.getTotalCount(key) } returns 10L

            val result = rankingService.getTopRankings(RankingPeriod.DAILY, date, page = 1, size = 3)

            assertThat(result.entries).hasSize(3)
            assertThat(result.entries[0].rank).isEqualTo(1)
            assertThat(result.entries[0].productId).isEqualTo(101L)
            assertThat(result.entries[2].rank).isEqualTo(3)
            assertThat(result.totalElements).isEqualTo(10)
        }

        @DisplayName("2페이지 조회 시 순위가 offset 에서 이어진다 (page=2, size=3 → rank 4,5,6)")
        @Test
        fun secondPageStartsFromOffset() {
            every { rankingRepository.getTopN(key, 3, 3) } returns listOf(
                RankingEntry(productId = 404L, score = 1.0),
                RankingEntry(productId = 505L, score = 0.8),
            )
            every { rankingRepository.getTotalCount(key) } returns 5L

            val result = rankingService.getTopRankings(RankingPeriod.DAILY, date, page = 2, size = 3)

            assertThat(result.entries[0].rank).isEqualTo(4)
            assertThat(result.entries[1].rank).isEqualTo(5)
            assertThat(result.page).isEqualTo(2)
        }

        @DisplayName("ZSET 에 데이터가 없으면 빈 랭킹을 반환한다")
        @Test
        fun returnsEmptyWhenNoData() {
            every { rankingRepository.getTopN(key, 0, 20) } returns emptyList()
            every { rankingRepository.getTotalCount(key) } returns 0L

            val result = rankingService.getTopRankings(RankingPeriod.DAILY, date, page = 1, size = 20)

            assertThat(result.entries).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }
    }

    @DisplayName("WEEKLY 랭킹 (MV 테이블 경로)")
    @Nested
    inner class WeeklyRankings {

        @DisplayName("요청 일자(date)가 속한 ISO 주의 월요일을 periodStart 로 사용한다")
        @Test
        fun normalizesDateToIsoMonday() {
            // 2026-04-15 (수) → 해당 주 월요일은 2026-04-13
            val wednesday = LocalDate.of(2026, 4, 15)
            val expectedMonday = LocalDate.of(2026, 4, 13)

            every { periodicRankingRepository.findTopWeekly(expectedMonday, 0, 20) } returns emptyList()
            every { periodicRankingRepository.countWeekly(expectedMonday) } returns 0L

            rankingService.getTopRankings(RankingPeriod.WEEKLY, wednesday, page = 1, size = 20)

            verify(exactly = 1) { periodicRankingRepository.findTopWeekly(expectedMonday, 0, 20) }
            verify(exactly = 1) { periodicRankingRepository.countWeekly(expectedMonday) }
        }

        @DisplayName("MV 가 돌려준 RankedProduct 를 그대로 RankingPage entries 에 담는다 (rankPosition 유지)")
        @Test
        fun passesThroughRankedProducts() {
            val monday = LocalDate.of(2026, 4, 13)
            every { periodicRankingRepository.findTopWeekly(monday, 0, 20) } returns listOf(
                RankedProduct(rank = 1L, productId = 10L, score = 99.9),
                RankedProduct(rank = 2L, productId = 20L, score = 88.8),
            )
            every { periodicRankingRepository.countWeekly(monday) } returns 2L

            val result = rankingService.getTopRankings(RankingPeriod.WEEKLY, monday, page = 1, size = 20)

            assertThat(result.entries).hasSize(2)
            assertThat(result.entries[0].rank).isEqualTo(1L)
            assertThat(result.entries[0].productId).isEqualTo(10L)
            assertThat(result.entries[0].score).isEqualTo(99.9)
            assertThat(result.totalElements).isEqualTo(2)
        }

        @DisplayName("요청한 주의 MV 데이터가 없으면 빈 페이지를 반환한다 (배치 미실행 케이스)")
        @Test
        fun returnsEmptyWhenMvNotPopulated() {
            val monday = LocalDate.of(2026, 4, 13)
            every { periodicRankingRepository.findTopWeekly(monday, 0, 20) } returns emptyList()
            every { periodicRankingRepository.countWeekly(monday) } returns 0L

            val result = rankingService.getTopRankings(RankingPeriod.WEEKLY, monday, page = 1, size = 20)

            assertThat(result.entries).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @DisplayName("page/size 를 올바른 offset 으로 변환해 MV 에 위임한다")
        @Test
        fun passesOffsetToRepository() {
            val monday = LocalDate.of(2026, 4, 13)
            every { periodicRankingRepository.findTopWeekly(monday, 20, 20) } returns emptyList()
            every { periodicRankingRepository.countWeekly(monday) } returns 0L

            rankingService.getTopRankings(RankingPeriod.WEEKLY, monday, page = 2, size = 20)

            verify(exactly = 1) { periodicRankingRepository.findTopWeekly(monday, 20, 20) }
        }
    }

    @DisplayName("MONTHLY 랭킹 (MV 테이블 경로)")
    @Nested
    inner class MonthlyRankings {

        @DisplayName("요청 일자가 속한 월의 yyyy-MM 으로 MV 를 조회한다")
        @Test
        fun normalizesDateToYearMonth() {
            // 2026-04-15 → "2026-04"
            val midMonth = LocalDate.of(2026, 4, 15)
            every { periodicRankingRepository.findTopMonthly("2026-04", 0, 20) } returns emptyList()
            every { periodicRankingRepository.countMonthly("2026-04") } returns 0L

            rankingService.getTopRankings(RankingPeriod.MONTHLY, midMonth, page = 1, size = 20)

            verify(exactly = 1) { periodicRankingRepository.findTopMonthly("2026-04", 0, 20) }
            verify(exactly = 1) { periodicRankingRepository.countMonthly("2026-04") }
        }

        @DisplayName("1월 같은 한 자리 월도 0-padded 'yyyy-MM' 으로 변환된다")
        @Test
        fun singleDigitMonthIsZeroPadded() {
            val january = LocalDate.of(2026, 1, 10)
            every { periodicRankingRepository.findTopMonthly("2026-01", 0, 20) } returns emptyList()
            every { periodicRankingRepository.countMonthly("2026-01") } returns 0L

            rankingService.getTopRankings(RankingPeriod.MONTHLY, january, page = 1, size = 20)

            verify(exactly = 1) { periodicRankingRepository.findTopMonthly("2026-01", 0, 20) }
        }

        @DisplayName("MV 결과를 RankingPage 로 래핑해 반환한다")
        @Test
        fun wrapsMvResultsIntoRankingPage() {
            val date = LocalDate.of(2026, 4, 15)
            every { periodicRankingRepository.findTopMonthly("2026-04", 0, 20) } returns listOf(
                RankedProduct(rank = 1L, productId = 1L, score = 123.4),
            )
            every { periodicRankingRepository.countMonthly("2026-04") } returns 1L

            val result = rankingService.getTopRankings(RankingPeriod.MONTHLY, date, page = 1, size = 20)

            assertThat(result.entries).hasSize(1)
            assertThat(result.entries[0].productId).isEqualTo(1L)
            assertThat(result.totalElements).isEqualTo(1)
        }
    }

    @DisplayName("getProductRank — 상품 상세용 일간 순위 조회")
    @Nested
    inner class GetProductRank {
        private val date = LocalDate.of(2026, 4, 10)
        private val key = "ranking:all:20260410"

        @DisplayName("ZSET 의 0-based 순위를 1-based 로 변환한다")
        @Test
        fun convertsZeroBasedToOneBased() {
            every { rankingRepository.getRank(key, 101L) } returns 0L

            val rank = rankingService.getProductRank(date, 101L)

            assertThat(rank).isEqualTo(1)
        }

        @DisplayName("순위에 없는 상품이면 null 을 반환한다")
        @Test
        fun returnsNullWhenProductNotRanked() {
            every { rankingRepository.getRank(key, 999L) } returns null

            val rank = rankingService.getProductRank(date, 999L)

            assertThat(rank).isNull()
        }
    }
}
