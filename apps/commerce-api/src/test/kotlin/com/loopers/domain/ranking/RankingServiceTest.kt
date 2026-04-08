package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("RankingService (commerce-api)")
class RankingServiceTest {

    @Mock
    private lateinit var rankingRepository: RankingRepository

    @InjectMocks
    private lateinit var rankingService: RankingService

    private val today = LocalDate.of(2026, 4, 9)

    @DisplayName("Top 랭킹 조회 시,")
    @Nested
    inner class GetTopRankings {

        @DisplayName("page=1, size=20이면 offset=0, count=20으로 Repository를 호출한다.")
        @Test
        fun calculatesOffsetForPage1() {
            // arrange
            whenever(rankingRepository.getTopRankings(eq(today), eq(0L), eq(20L)))
                .thenReturn(emptyList())

            // act
            rankingService.getTopRankings(today, page = 1, size = 20)

            // assert
            verify(rankingRepository).getTopRankings(eq(today), eq(0L), eq(20L))
        }

        @DisplayName("page=2, size=20이면 offset=20, count=20으로 Repository를 호출한다.")
        @Test
        fun calculatesOffsetForPage2() {
            // arrange
            whenever(rankingRepository.getTopRankings(eq(today), eq(20L), eq(20L)))
                .thenReturn(emptyList())

            // act
            rankingService.getTopRankings(today, page = 2, size = 20)

            // assert
            verify(rankingRepository).getTopRankings(eq(today), eq(20L), eq(20L))
        }
    }

    @DisplayName("특정 상품 순위 조회 시,")
    @Nested
    inner class GetRank {

        @DisplayName("랭킹에 진입한 상품은 순위를 반환한다.")
        @Test
        fun returnsRankForExistingProduct() {
            // arrange
            whenever(rankingRepository.getRank(today, 100L)).thenReturn(0L)

            // act
            val rank = rankingService.getRank(today, 100L)

            // assert
            assertThat(rank).isEqualTo(0L)
        }

        @DisplayName("랭킹에 진입하지 않은 상품은 null을 반환한다.")
        @Test
        fun returnsNullForNonExistingProduct() {
            // arrange
            whenever(rankingRepository.getRank(today, 999L)).thenReturn(null)

            // act
            val rank = rankingService.getRank(today, 999L)

            // assert
            assertThat(rank).isNull()
        }
    }
}
