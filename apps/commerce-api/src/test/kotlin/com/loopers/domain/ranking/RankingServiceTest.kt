package com.loopers.domain.ranking

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("RankingService")
class RankingServiceTest {

    private val rankingRepository: RankingRepository = mockk()
    private val rankingService = RankingService(rankingRepository)

    private val date = LocalDate.of(2026, 4, 10)
    private val key = "ranking:all:20260410"

    @DisplayName("getTopRankings는 ZSET에서 score 내림차순으로 상품을 조회하고 1-based 순위를 부여한다")
    @Test
    fun returnsTopRankingsWithOneBasedRank() {
        // arrange
        every { rankingRepository.getTopN(key, 0, 3) } returns listOf(
            RankingEntry(productId = 101L, score = 5.0),
            RankingEntry(productId = 202L, score = 3.5),
            RankingEntry(productId = 303L, score = 1.2),
        )
        every { rankingRepository.getTotalCount(key) } returns 10L

        // act
        val result = rankingService.getTopRankings(date, page = 1, size = 3)

        // assert
        assertThat(result.entries).hasSize(3)
        assertThat(result.entries[0].rank).isEqualTo(1)
        assertThat(result.entries[0].productId).isEqualTo(101L)
        assertThat(result.entries[1].rank).isEqualTo(2)
        assertThat(result.entries[2].rank).isEqualTo(3)
        assertThat(result.totalElements).isEqualTo(10)
    }

    @DisplayName("2페이지 조회 시 순위가 offset에서 이어진다 (page=2, size=3 → rank 4,5,6)")
    @Test
    fun secondPageStartsFromOffset() {
        // arrange
        every { rankingRepository.getTopN(key, 3, 3) } returns listOf(
            RankingEntry(productId = 404L, score = 1.0),
            RankingEntry(productId = 505L, score = 0.8),
        )
        every { rankingRepository.getTotalCount(key) } returns 5L

        // act
        val result = rankingService.getTopRankings(date, page = 2, size = 3)

        // assert
        assertThat(result.entries[0].rank).isEqualTo(4)
        assertThat(result.entries[1].rank).isEqualTo(5)
        assertThat(result.page).isEqualTo(2)
    }

    @DisplayName("ZSET에 데이터가 없으면 빈 랭킹을 반환한다")
    @Test
    fun returnsEmptyWhenNoData() {
        every { rankingRepository.getTopN(key, 0, 20) } returns emptyList()
        every { rankingRepository.getTotalCount(key) } returns 0L

        val result = rankingService.getTopRankings(date, page = 1, size = 20)

        assertThat(result.entries).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    @DisplayName("getProductRank는 ZSET의 0-based 순위를 1-based로 변환한다")
    @Test
    fun convertsZeroBasedToOneBased() {
        every { rankingRepository.getRank(key, 101L) } returns 0L

        val rank = rankingService.getProductRank(date, 101L)

        assertThat(rank).isEqualTo(1)
    }

    @DisplayName("getProductRank는 순위에 없는 상품이면 null을 반환한다")
    @Test
    fun returnsNullWhenProductNotRanked() {
        every { rankingRepository.getRank(key, 999L) } returns null

        val rank = rankingService.getProductRank(date, 999L)

        assertThat(rank).isNull()
    }
}
