package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RankingServiceTest {
    private lateinit var rankingRepository: RankingRepository
    private lateinit var rankingService: RankingService

    private val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    private val todayKey = "ranking:all:$today"

    @BeforeEach
    fun setUp() {
        rankingRepository = mock()
        rankingService = RankingService(rankingRepository)
    }

    @DisplayName("랭킹을 조회할 때, ")
    @Nested
    inner class GetRanking {
        @DisplayName("page와 size로 올바른 범위를 요청한다.")
        @Test
        fun requestsCorrectRange() {
            // arrange
            val entries = listOf(
                RankingEntry(productId = 1L, score = 100.0),
                RankingEntry(productId = 2L, score = 50.0),
            )
            whenever(rankingRepository.getTopNWithScores(any(), eq(0L), eq(19L))).thenReturn(entries)

            // act
            val result = rankingService.getRanking(today, page = 1, size = 20)

            // assert
            assertThat(result).hasSize(2)
            assertThat(result[0].productId).isEqualTo(1L)
        }

        @DisplayName("2페이지 요청 시 올바른 offset으로 조회한다.")
        @Test
        fun requestsCorrectOffsetForPage2() {
            // arrange
            whenever(rankingRepository.getTopNWithScores(any(), eq(20L), eq(39L))).thenReturn(emptyList())

            // act
            val result = rankingService.getRanking(today, page = 2, size = 20)

            // assert
            assertThat(result).isEmpty()
        }
    }

    @DisplayName("상품 순위를 조회할 때, ")
    @Nested
    inner class GetProductRank {
        @DisplayName("순위가 있으면 1-based로 반환한다.")
        @Test
        fun returns1BasedRank() {
            // arrange
            whenever(rankingRepository.getRank(todayKey, 1L)).thenReturn(0L)

            // act
            val rank = rankingService.getProductRank(1L)

            // assert
            assertThat(rank).isEqualTo(1)
        }

        @DisplayName("순위가 없으면 null을 반환한다.")
        @Test
        fun returnsNull_whenNotRanked() {
            // arrange
            whenever(rankingRepository.getRank(todayKey, 999L)).thenReturn(null)

            // act
            val rank = rankingService.getProductRank(999L)

            // assert
            assertThat(rank).isNull()
        }
    }
}
