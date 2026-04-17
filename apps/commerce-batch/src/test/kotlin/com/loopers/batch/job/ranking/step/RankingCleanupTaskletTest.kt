package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.ProductRankMonthlyRepository
import com.loopers.domain.ranking.ProductRankWeeklyRepository
import com.loopers.domain.ranking.RankingPeriodType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.repeat.RepeatStatus
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class RankingCleanupTaskletTest {

    @Mock
    private lateinit var weeklyRepository: ProductRankWeeklyRepository

    @Mock
    private lateinit var monthlyRepository: ProductRankMonthlyRepository

    @Mock
    private lateinit var stepContribution: StepContribution

    @Mock
    private lateinit var chunkContext: ChunkContext

    @Nested
    @DisplayName("기간별 기존 데이터 삭제")
    inner class CleanupByPeriod {

        @DisplayName("WEEKLY일 때 해당 주 월요일 기준으로 weeklyRepository를 호출한다")
        @Test
        fun execute_whenWeekly_deletesWeeklyData() {
            // arrange
            val requestDate = LocalDate.of(2026, 4, 15)
            val tasklet = RankingCleanupTasklet(RankingPeriodType.WEEKLY, requestDate, weeklyRepository, monthlyRepository)

            // act
            val status = tasklet.execute(stepContribution, chunkContext)

            // assert
            verify(weeklyRepository).deleteByPeriodStartDate(eq(LocalDate.of(2026, 4, 13)))
            verifyNoInteractions(monthlyRepository)
            assertThat(status).isEqualTo(RepeatStatus.FINISHED)
        }

        @DisplayName("MONTHLY일 때 해당 월 1일 기준으로 monthlyRepository를 호출한다")
        @Test
        fun execute_whenMonthly_deletesMonthlyData() {
            // arrange
            val requestDate = LocalDate.of(2026, 4, 15)
            val tasklet = RankingCleanupTasklet(RankingPeriodType.MONTHLY, requestDate, weeklyRepository, monthlyRepository)

            // act
            val status = tasklet.execute(stepContribution, chunkContext)

            // assert
            verify(monthlyRepository).deleteByPeriodStartDate(eq(LocalDate.of(2026, 4, 1)))
            verifyNoInteractions(weeklyRepository)
            assertThat(status).isEqualTo(RepeatStatus.FINISHED)
        }
    }
}
