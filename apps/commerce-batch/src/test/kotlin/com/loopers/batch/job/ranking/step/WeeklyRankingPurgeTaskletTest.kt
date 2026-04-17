package com.loopers.batch.job.ranking.step

import com.loopers.infrastructure.mv.WeeklyProductRankJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.repeat.RepeatStatus
import java.time.LocalDate

@DisplayName("WeeklyRankingPurgeTasklet — 해당 주의 MV 행 삭제")
class WeeklyRankingPurgeTaskletTest {

    @DisplayName("requestDate 가 속한 주의 월요일을 기준으로 deleteByPeriodStart 를 호출한다")
    @Test
    fun deletesByMondayOfWeek() {
        val repository = mockk<WeeklyProductRankJpaRepository>(relaxed = true)
        every { repository.deleteByPeriodStart(any()) } returns 0
        // 2026-04-17 (금) → 주의 월요일은 2026-04-13
        val tasklet = WeeklyRankingPurgeTasklet(repository, requestDate = "20260417")

        val status = tasklet.execute(mockk(relaxed = true), mockk<ChunkContext>(relaxed = true))

        assertThat(status).isEqualTo(RepeatStatus.FINISHED)
        verify(exactly = 1) { repository.deleteByPeriodStart(LocalDate.of(2026, 4, 13)) }
    }

    @DisplayName("월요일을 전달하면 그 월요일이 그대로 삭제 기준이 된다")
    @Test
    fun mondayInputDeletesBySameMonday() {
        val repository = mockk<WeeklyProductRankJpaRepository>(relaxed = true)
        every { repository.deleteByPeriodStart(any()) } returns 0
        val tasklet = WeeklyRankingPurgeTasklet(repository, requestDate = "20260413")

        tasklet.execute(mockk(relaxed = true), mockk<ChunkContext>(relaxed = true))

        verify(exactly = 1) { repository.deleteByPeriodStart(LocalDate.of(2026, 4, 13)) }
    }

    @DisplayName("일요일을 전달해도 그 주 월요일을 기준으로 삭제한다")
    @Test
    fun sundayInputNormalizesToMonday() {
        val repository = mockk<WeeklyProductRankJpaRepository>(relaxed = true)
        every { repository.deleteByPeriodStart(any()) } returns 0
        val tasklet = WeeklyRankingPurgeTasklet(repository, requestDate = "20260419")

        tasklet.execute(mockk(relaxed = true), mockk<ChunkContext>(relaxed = true))

        verify(exactly = 1) { repository.deleteByPeriodStart(LocalDate.of(2026, 4, 13)) }
    }
}
