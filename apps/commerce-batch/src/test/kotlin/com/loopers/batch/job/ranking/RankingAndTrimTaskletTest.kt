package com.loopers.batch.job.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.util.ReflectionTestUtils

class RankingAndTrimTaskletTest {

    private val jdbcTemplate: JdbcTemplate = mock()
    private val contribution: StepContribution = mock()
    private val chunkContext: ChunkContext = mock()

    // --- Weekly ---

    private lateinit var weeklyTasklet: WeeklyRankingAndTrimTasklet

    @BeforeEach
    fun setUp() {
        weeklyTasklet = WeeklyRankingAndTrimTasklet(jdbcTemplate)
        // 2026-04-14 → W16
        ReflectionTestUtils.setField(weeklyTasklet, "targetDate", "20260414")
    }

    @DisplayName("[주간] 랭크 부여 SQL + 트리밍 SQL을 순서대로 실행")
    @Test
    fun weeklyTaskletShouldAssignRankAndTrim() {
        val result = weeklyTasklet.execute(contribution, chunkContext)

        val sqlCaptor = argumentCaptor<String>()
        val paramCaptor = argumentCaptor<Any>()
        verify(jdbcTemplate, times(2)).update(sqlCaptor.capture(), paramCaptor.capture())

        val sqls = sqlCaptor.allValues
        assertThat(sqls[0]).containsIgnoringCase("ROW_NUMBER")
        assertThat(sqls[1]).containsIgnoringCase("rank > 100")

        // 두 SQL 모두 "2026-W16" 파라미터 사용
        assertThat(paramCaptor.allValues).allMatch { it == "2026-W16" }
        assertThat(result).isEqualTo(RepeatStatus.FINISHED)
    }

    @DisplayName("[주간] yearWeek 형식 검증: 2026-W16")
    @Test
    fun weeklyTaskletShouldUseCorrectYearWeek() {
        weeklyTasklet.execute(contribution, chunkContext)

        verify(jdbcTemplate, times(2)).update(any<String>(), eq("2026-W16"))
    }

    // --- Monthly ---

    private lateinit var monthlyTasklet: MonthlyRankingAndTrimTasklet

    @BeforeEach
    fun setUpMonthly() {
        monthlyTasklet = MonthlyRankingAndTrimTasklet(jdbcTemplate)
        // 2026-04-14 → 2026-04
        ReflectionTestUtils.setField(monthlyTasklet, "targetDate", "20260414")
    }

    @DisplayName("[월간] 랭크 부여 SQL + 트리밍 SQL을 순서대로 실행")
    @Test
    fun monthlyTaskletShouldAssignRankAndTrim() {
        val result = monthlyTasklet.execute(contribution, chunkContext)

        val sqlCaptor = argumentCaptor<String>()
        verify(jdbcTemplate, times(2)).update(sqlCaptor.capture(), any<String>())

        val sqls = sqlCaptor.allValues
        assertThat(sqls[0]).containsIgnoringCase("ROW_NUMBER")
        assertThat(sqls[1]).containsIgnoringCase("rank > 100")
        assertThat(result).isEqualTo(RepeatStatus.FINISHED)
    }

    @DisplayName("[월간] yearMonth 형식 검증: 2026-04")
    @Test
    fun monthlyTaskletShouldUseCorrectYearMonth() {
        monthlyTasklet.execute(contribution, chunkContext)

        verify(jdbcTemplate, times(2)).update(any<String>(), eq("2026-04"))
    }
}
