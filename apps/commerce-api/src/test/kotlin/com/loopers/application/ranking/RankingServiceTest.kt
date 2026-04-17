package com.loopers.application.ranking

import com.loopers.domain.ranking.RankingEntry
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RankingService 단위 테스트")
class RankingServiceTest {

    private val rankingStore: RankingStore = mockk()
    private val mvRankingStore: MvRankingStore = mockk()
    private val rankingService = RankingService(rankingStore, mvRankingStore)

    @Nested
    @DisplayName("getTopRankings")
    inner class GetTopRankings {

        @Test
        @DisplayName("일간 랭킹을 페이지 단위로 조회한다")
        fun `일간 랭킹 조회`() {
            // Arrange
            val entries = listOf(
                RankingEntry(101L, 100.0),
                RankingEntry(202L, 50.0),
            )
            every { rankingStore.getTopProducts("ranking:all:20250907", 0, 20) } returns entries
            every { rankingStore.getTotalCount("ranking:all:20250907") } returns 2L

            // Act
            val result = rankingService.getTopRankings("20250907", 0, 20)

            // Assert
            assertThat(result.entries).hasSize(2)
            assertThat(result.entries[0].rank).isEqualTo(1)
            assertThat(result.entries[0].productId).isEqualTo(101L)
            assertThat(result.entries[1].rank).isEqualTo(2)
            assertThat(result.totalElements).isEqualTo(2)
            assertThat(result.totalPages).isEqualTo(1)
        }

        @Test
        @DisplayName("2페이지 조회 시 rank가 올바르게 계산된다")
        fun `2페이지 rank 계산`() {
            // Arrange
            val entries = listOf(
                RankingEntry(303L, 30.0),
            )
            every { rankingStore.getTopProducts("ranking:all:20250907", 20, 20) } returns entries
            every { rankingStore.getTotalCount("ranking:all:20250907") } returns 21L

            // Act
            val result = rankingService.getTopRankings("20250907", 1, 20)

            // Assert
            assertThat(result.entries[0].rank).isEqualTo(21)
            assertThat(result.totalPages).isEqualTo(2)
        }

        @Test
        @DisplayName("빈 랭킹 조회 시 빈 결과 반환")
        fun `빈 랭킹`() {
            // Arrange
            every { rankingStore.getTopProducts("ranking:all:20250907", 0, 20) } returns emptyList()
            every { rankingStore.getTotalCount("ranking:all:20250907") } returns 0L

            // Act
            val result = rankingService.getTopRankings("20250907", 0, 20)

            // Assert
            assertThat(result.entries).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
            assertThat(result.totalPages).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("getHourlyTopRankings")
    inner class GetHourlyTopRankings {

        @Test
        @DisplayName("시간 단위 랭킹을 조회한다")
        fun `시간 랭킹 조회`() {
            // Arrange
            val entries = listOf(RankingEntry(101L, 10.0))
            every { rankingStore.getTopProducts("ranking:all:20250907:14", 0, 10) } returns entries
            every { rankingStore.getTotalCount("ranking:all:20250907:14") } returns 1L

            // Act
            val result = rankingService.getHourlyTopRankings("20250907", "14", 0, 10)

            // Assert
            assertThat(result.entries).hasSize(1)
            assertThat(result.entries[0].rank).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("getRank")
    inner class GetRank {

        @Test
        @DisplayName("상품의 1-based 순위와 점수를 반환한다")
        fun `순위 조회`() {
            // Arrange
            every { rankingStore.getRank("ranking:all:20250907", 101L) } returns 0L
            every { rankingStore.getScore("ranking:all:20250907", 101L) } returns 100.0

            // Act
            val result = rankingService.getRank("20250907", 101L)

            // Assert
            assertThat(result).isNotNull
            assertThat(result!!.rank).isEqualTo(1)
            assertThat(result.score).isEqualTo(100.0)
        }

        @Test
        @DisplayName("랭킹에 없는 상품은 null 반환")
        fun `없는 상품`() {
            // Arrange
            every { rankingStore.getRank("ranking:all:20250907", 999L) } returns null

            // Act
            val result = rankingService.getRank("20250907", 999L)

            // Assert
            assertThat(result).isNull()
        }
    }
}
