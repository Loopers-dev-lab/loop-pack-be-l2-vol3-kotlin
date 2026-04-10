package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RankingServiceTest {

    private lateinit var rankingRepository: FakeRankingRepository
    private lateinit var rankingService: RankingService

    private val dateKey = "20260408"
    private val hourKey = "2026040814"

    @BeforeEach
    fun setUp() {
        rankingRepository = FakeRankingRepository()
        rankingService = RankingService(rankingRepository)
    }

    @Nested
    @DisplayName("getTopRankings — DAILY")
    inner class GetTopRankingsDaily {

        @Test
        @DisplayName("score 내림차순으로 1-based 순위가 부여된 결과를 반환한다")
        fun returnsRankedResults() {
            // arrange
            rankingRepository.addScore(RankingWindow.DAILY, dateKey, 1L, 50.0)
            rankingRepository.addScore(RankingWindow.DAILY, dateKey, 2L, 30.0)
            rankingRepository.addScore(RankingWindow.DAILY, dateKey, 3L, 70.0)

            // act
            val result = rankingService.getTopRankings(RankingWindow.DAILY, dateKey, page = 0, size = 10)

            // assert
            assertThat(result.rankings).hasSize(3)
            assertThat(result.rankings[0].productId).isEqualTo(3L)
            assertThat(result.rankings[0].rank).isEqualTo(1L)
            assertThat(result.rankings[1].productId).isEqualTo(1L)
            assertThat(result.rankings[1].rank).isEqualTo(2L)
            assertThat(result.rankings[2].productId).isEqualTo(2L)
            assertThat(result.rankings[2].rank).isEqualTo(3L)
            assertThat(result.totalCount).isEqualTo(3L)
        }

        @Test
        @DisplayName("빈 ZSET이면 빈 결과를 반환한다")
        fun emptyZSetReturnsEmpty() {
            // act
            val result = rankingService.getTopRankings(RankingWindow.DAILY, dateKey, page = 0, size = 20)

            // assert
            assertThat(result.rankings).isEmpty()
            assertThat(result.totalCount).isEqualTo(0L)
        }

        @Test
        @DisplayName("페이징이 적용되어 해당 범위의 순위만 반환한다")
        fun paginationWorks() {
            // arrange — 5개 상품
            (1L..5L).forEach { id ->
                rankingRepository.addScore(RankingWindow.DAILY, dateKey, id, (6 - id).toDouble())
            }

            // act — page=1, size=2 → 3등~4등
            val result = rankingService.getTopRankings(RankingWindow.DAILY, dateKey, page = 1, size = 2)

            // assert
            assertThat(result.rankings).hasSize(2)
            assertThat(result.rankings[0].rank).isEqualTo(3L)
            assertThat(result.rankings[1].rank).isEqualTo(4L)
        }
    }

    @Nested
    @DisplayName("getTopRankings — HOURLY")
    inner class GetTopRankingsHourly {

        @Test
        @DisplayName("시간 윈도우는 DAILY와 다른 결과를 반환한다")
        fun hourlyIsIndependentFromDaily() {
            // arrange — 같은 상품이지만 DAILY와 HOURLY에 서로 다른 score 저장
            rankingRepository.addScore(RankingWindow.DAILY, dateKey, 1L, 100.0)
            rankingRepository.addScore(RankingWindow.HOURLY, hourKey, 1L, 10.0)
            rankingRepository.addScore(RankingWindow.HOURLY, hourKey, 2L, 20.0)

            // act
            val dailyResult = rankingService.getTopRankings(RankingWindow.DAILY, dateKey, page = 0, size = 10)
            val hourlyResult = rankingService.getTopRankings(RankingWindow.HOURLY, hourKey, page = 0, size = 10)

            // assert
            assertThat(dailyResult.rankings).hasSize(1)
            assertThat(dailyResult.rankings[0].productId).isEqualTo(1L)
            assertThat(dailyResult.rankings[0].score).isEqualTo(100.0)

            assertThat(hourlyResult.rankings).hasSize(2)
            assertThat(hourlyResult.rankings[0].productId).isEqualTo(2L)
            assertThat(hourlyResult.rankings[0].score).isEqualTo(20.0)
        }
    }

    @Nested
    @DisplayName("getProductRank")
    inner class GetProductRank {

        @Test
        @DisplayName("존재하는 상품의 1-based 순위를 반환한다")
        fun returnsRankForExistingProduct() {
            // arrange
            rankingRepository.addScore(RankingWindow.DAILY, dateKey, 1L, 50.0)
            rankingRepository.addScore(RankingWindow.DAILY, dateKey, 2L, 30.0)
            rankingRepository.addScore(RankingWindow.DAILY, dateKey, 3L, 70.0)

            // act
            val result = rankingService.getProductRank(dateKey, 1L)

            // assert — productId=1은 score 50으로 2등
            assertThat(result.rank).isEqualTo(2L)
            assertThat(result.score).isEqualTo(50.0)
        }

        @Test
        @DisplayName("존재하지 않는 상품은 rank=null, score=null을 반환한다")
        fun returnsNullForMissingProduct() {
            // arrange
            rankingRepository.addScore(RankingWindow.DAILY, dateKey, 1L, 50.0)

            // act
            val result = rankingService.getProductRank(dateKey, 99L)

            // assert
            assertThat(result.rank).isNull()
            assertThat(result.score).isNull()
        }
    }
}
