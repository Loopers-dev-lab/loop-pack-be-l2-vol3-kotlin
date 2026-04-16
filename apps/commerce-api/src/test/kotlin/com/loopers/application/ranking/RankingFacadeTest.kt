package com.loopers.application.ranking

import com.loopers.domain.ranking.Period
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
@DisplayName("RankingFacade")
class RankingFacadeTest {

    private val dailyStrategy: RankingStrategy = mock()
    private val weeklyStrategy: RankingStrategy = mock()
    private val monthlyStrategy: RankingStrategy = mock()

    private val rankingFacade = RankingFacade(
        strategies = mapOf(
            DailyRankingStrategy.BEAN_NAME to dailyStrategy,
            WeeklyRankingStrategy.BEAN_NAME to weeklyStrategy,
            MonthlyRankingStrategy.BEAN_NAME to monthlyStrategy,
        ),
    )

    private val today = LocalDate.of(2026, 4, 9)

    @DisplayName("랭킹 조회 시,")
    @Nested
    inner class GetRankings {

        @DisplayName("DAILY period 요청 시 DailyRankingStrategy에 위임한다.")
        @Test
        fun delegatesToDailyStrategy() {
            // arrange
            val expected = RankingResult(
                period = Period.DAILY,
                periodStart = "2026-04-09",
                periodEnd = "2026-04-09",
                items = listOf(RankingInfo(100L, 1L, 80.0, "상품A", 10000L)),
            )
            whenever(dailyStrategy.getRankings(today, 1, 20)).thenReturn(expected)

            // act
            val result = rankingFacade.getRankings(today, Period.DAILY, 1, 20)

            // assert
            assertThat(result).isEqualTo(expected)
        }

        @DisplayName("WEEKLY period 요청 시 WeeklyRankingStrategy에 위임한다.")
        @Test
        fun delegatesToWeeklyStrategy() {
            // arrange
            val expected = RankingResult(
                period = Period.WEEKLY,
                periodStart = "2026-04-06",
                periodEnd = "2026-04-12",
                items = listOf(RankingInfo(200L, 1L, 44.0, "상품B", 20000L)),
            )
            whenever(weeklyStrategy.getRankings(today, 1, 20)).thenReturn(expected)

            // act
            val result = rankingFacade.getRankings(today, Period.WEEKLY, 1, 20)

            // assert
            assertThat(result).isEqualTo(expected)
        }

        @DisplayName("MONTHLY period 요청 시 MonthlyRankingStrategy에 위임한다.")
        @Test
        fun delegatesToMonthlyStrategy() {
            // arrange
            val expected = RankingResult(
                period = Period.MONTHLY,
                periodStart = "2026-04-01",
                periodEnd = "2026-04-30",
                items = listOf(RankingInfo(300L, 1L, 100.0, "상품C", 30000L)),
            )
            whenever(monthlyStrategy.getRankings(today, 1, 20)).thenReturn(expected)

            // act
            val result = rankingFacade.getRankings(today, Period.MONTHLY, 1, 20)

            // assert
            assertThat(result).isEqualTo(expected)
        }
    }
}
