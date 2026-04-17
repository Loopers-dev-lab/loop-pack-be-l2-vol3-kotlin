package com.loopers.batch.job.ranking.step

import com.loopers.infrastructure.mv.MonthlyProductRankJpaRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.repeat.RepeatStatus

@DisplayName("MonthlyRankingPurgeTasklet — 해당 월의 MV 행 삭제")
class MonthlyRankingPurgeTaskletTest {

    @DisplayName("requestDate 가 속한 월의 yearMonthVal 로 deleteByYearMonthVal 을 호출한다")
    @Test
    fun deletesByYearMonthOfRequestDate() {
        val repository = mockk<MonthlyProductRankJpaRepository>(relaxed = true)
        every { repository.deleteByYearMonthVal(any()) } returns 0
        val tasklet = MonthlyRankingPurgeTasklet(repository, requestDate = "20260415")

        val status = tasklet.execute(mockk(relaxed = true), mockk<ChunkContext>(relaxed = true))

        assertThat(status).isEqualTo(RepeatStatus.FINISHED)
        verify(exactly = 1) { repository.deleteByYearMonthVal("2026-04") }
    }

    @DisplayName("한 자리 월은 0-padded yearMonthVal 로 삭제된다")
    @Test
    fun singleDigitMonthIsZeroPadded() {
        val repository = mockk<MonthlyProductRankJpaRepository>(relaxed = true)
        every { repository.deleteByYearMonthVal(any()) } returns 0
        val tasklet = MonthlyRankingPurgeTasklet(repository, requestDate = "20260115")

        tasklet.execute(mockk(relaxed = true), mockk<ChunkContext>(relaxed = true))

        verify(exactly = 1) { repository.deleteByYearMonthVal("2026-01") }
    }

    @DisplayName("윤년 2월 날짜를 전달해도 해당 월 전체를 삭제한다")
    @Test
    fun leapFebruaryTargetsFebruary() {
        val repository = mockk<MonthlyProductRankJpaRepository>(relaxed = true)
        every { repository.deleteByYearMonthVal(any()) } returns 0
        val tasklet = MonthlyRankingPurgeTasklet(repository, requestDate = "20240229")

        tasklet.execute(mockk(relaxed = true), mockk<ChunkContext>(relaxed = true))

        verify(exactly = 1) { repository.deleteByYearMonthVal("2024-02") }
    }
}
