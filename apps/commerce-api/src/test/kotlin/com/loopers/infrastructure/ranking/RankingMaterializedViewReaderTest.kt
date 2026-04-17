package com.loopers.infrastructure.ranking

import com.loopers.application.ranking.RankingPeriod
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.LocalDate

class RankingMaterializedViewReaderTest {
    private val weeklyRepository = mockk<WeeklyProductRankingJpaRepository>()
    private val monthlyRepository = mockk<MonthlyProductRankingJpaRepository>()
    private val reader = RankingMaterializedViewReader(weeklyRepository, monthlyRepository)

    @Test
    fun `주간_MV는_주_시작일로_조회한다`() {
        every {
            weeklyRepository.findAllByWeekStartDateOrderByRankingAsc(LocalDate.parse("2026-04-13"), PageRequest.of(0, 2))
        } returns PageImpl(
            listOf(
                WeeklyProductRankingEntity(
                    weekStartDate = LocalDate.parse("2026-04-13"),
                    weekEndDate = LocalDate.parse("2026-04-19"),
                    productId = 1L,
                    ranking = 1L,
                    score = 10.0,
                    likeCount = 0L,
                    viewCount = 0L,
                    salesCount = 10L,
                ),
            ),
        )

        val result = reader.getPage(RankingPeriod.WEEKLY, "20260416", 2, 1)

        assertThat(result).containsExactly(RankedProductScore(rank = 1L, productId = 1L, score = 10.0))
    }

    @Test
    fun `월간_MV는_월_시작일로_조회한다`() {
        every {
            monthlyRepository.findAllByMonthStartDateOrderByRankingAsc(LocalDate.parse("2026-04-01"), PageRequest.of(0, 1))
        } returns PageImpl(
            listOf(
                MonthlyProductRankingEntity(
                    monthStartDate = LocalDate.parse("2026-04-01"),
                    monthEndDate = LocalDate.parse("2026-04-30"),
                    productId = 2L,
                    ranking = 1L,
                    score = 8.0,
                    likeCount = 0L,
                    viewCount = 0L,
                    salesCount = 8L,
                ),
            ),
        )

        val result = reader.getPage(RankingPeriod.MONTHLY, "20260416", 1, 1)

        assertThat(result).containsExactly(RankedProductScore(rank = 1L, productId = 2L, score = 8.0))
    }
}
