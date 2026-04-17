package com.loopers.batch.job.ranking.step

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.repeat.RepeatStatus
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class RankingCleanupTaskletTest {

    @Mock
    private lateinit var stepContribution: StepContribution

    @Mock
    private lateinit var chunkContext: ChunkContext

    @Nested
    @DisplayName("기간별 기존 데이터 삭제")
    inner class CleanupByPeriod {

        @DisplayName("전달받은 periodStartDate로 deleteAction을 호출한다")
        @Test
        fun execute_callsDeleteActionWithCorrectDate() {
            // arrange
            val deletedDates = mutableListOf<LocalDate>()
            val periodStartDate = LocalDate.of(2026, 4, 13)
            val tasklet = RankingCleanupTasklet("WEEKLY", periodStartDate) { date -> deletedDates.add(date) }

            // act
            val status = tasklet.execute(stepContribution, chunkContext)

            // assert
            assertThat(status).isEqualTo(RepeatStatus.FINISHED)
            assertThat(deletedDates).containsExactly(periodStartDate)
        }

        @DisplayName("MONTHLY 기간의 1일 기준으로 삭제한다")
        @Test
        fun execute_whenMonthly_deletesWithFirstDayOfMonth() {
            // arrange
            val deletedDates = mutableListOf<LocalDate>()
            val periodStartDate = LocalDate.of(2026, 4, 1)
            val tasklet = RankingCleanupTasklet("MONTHLY", periodStartDate) { date -> deletedDates.add(date) }

            // act
            val status = tasklet.execute(stepContribution, chunkContext)

            // assert
            assertThat(status).isEqualTo(RepeatStatus.FINISHED)
            assertThat(deletedDates).containsExactly(periodStartDate)
        }
    }
}
