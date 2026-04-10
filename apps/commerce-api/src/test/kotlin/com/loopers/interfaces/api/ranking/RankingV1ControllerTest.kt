package com.loopers.interfaces.api.ranking

import com.loopers.application.ranking.RankingPageInfo
import com.loopers.application.ranking.RankingService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class RankingV1ControllerTest {

    @Mock
    private lateinit var rankingService: RankingService

    @InjectMocks
    private lateinit var controller: RankingV1Controller

    @DisplayName("랭킹을 조회할 때,")
    @Nested
    inner class GetRankings {

        @DisplayName("date 파라미터가 없으면, 오늘 날짜로 조회한다.")
        @Test
        fun usesTodayDate_whenDateIsNull() {
            // arrange
            val pageInfo = RankingPageInfo(rankings = emptyList(), totalCount = 0L)
            whenever(rankingService.getRankings(eq(LocalDate.now()), eq(1), eq(20))).thenReturn(pageInfo)

            // act
            val result = controller.getRankings(null, 20, 1)

            // assert
            assertThat(result.data?.date).isEqualTo(LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE))
        }

        @DisplayName("date 파라미터가 있으면, 해당 날짜로 조회한다.")
        @Test
        fun usesSpecifiedDate_whenDateProvided() {
            // arrange
            val pageInfo = RankingPageInfo(rankings = emptyList(), totalCount = 0L)
            whenever(rankingService.getRankings(eq(LocalDate.of(2026, 4, 7)), eq(1), eq(20))).thenReturn(pageInfo)

            // act
            val result = controller.getRankings("20260407", 20, 1)

            // assert
            assertThat(result.data?.date).isEqualTo("20260407")
        }
    }
}
