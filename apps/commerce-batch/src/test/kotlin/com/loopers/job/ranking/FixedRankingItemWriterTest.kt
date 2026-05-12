package com.loopers.job.ranking

import com.loopers.batch.job.ranking.FixedRankingAggregateRow
import com.loopers.batch.job.ranking.step.MonthlyFixedRankingItemWriter
import com.loopers.batch.job.ranking.step.WeeklyFixedRankingItemWriter
import com.loopers.domain.ranking.ProductRankMonthlyMvModel
import com.loopers.domain.ranking.ProductRankWeeklyMvModel
import com.loopers.infrastructure.ranking.ProductRankMonthlyMvJpaRepository
import com.loopers.infrastructure.ranking.ProductRankWeeklyMvJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.batch.item.Chunk
import java.time.LocalDate

class FixedRankingItemWriterTest {
    @Test
    fun `weekly writer는 chunk 순서대로 rank를 부여한다`() {
        val repository: ProductRankWeeklyMvJpaRepository = mock()
        val writer = WeeklyFixedRankingItemWriter(
            periodStartDate = LocalDate.of(2026, 4, 13),
            periodEndDate = LocalDate.of(2026, 4, 19),
            productRankWeeklyMvJpaRepository = repository,
        )

        writer.write(Chunk(listOf(FixedRankingAggregateRow(10L, 9.0), FixedRankingAggregateRow(11L, 8.0))))

        val captor = argumentCaptor<List<ProductRankWeeklyMvModel>>()
        verify(repository).saveAll(captor.capture())
        assertThat(captor.firstValue.map { it.rankPosition }).containsExactly(1L, 2L)
        assertThat(captor.firstValue.map { it.productId }).containsExactly(10L, 11L)
    }

    @Test
    fun `monthly writer는 chunk 순서대로 rank를 부여한다`() {
        val repository: ProductRankMonthlyMvJpaRepository = mock()
        val writer = MonthlyFixedRankingItemWriter(
            periodStartDate = LocalDate.of(2026, 4, 1),
            periodEndDate = LocalDate.of(2026, 4, 30),
            productRankMonthlyMvJpaRepository = repository,
        )

        writer.write(Chunk(listOf(FixedRankingAggregateRow(20L, 12.0), FixedRankingAggregateRow(21L, 3.0))))

        val captor = argumentCaptor<List<ProductRankMonthlyMvModel>>()
        verify(repository).saveAll(captor.capture())
        assertThat(captor.firstValue.map { it.rankPosition }).containsExactly(1L, 2L)
        assertThat(captor.firstValue.map { it.productId }).containsExactly(20L, 21L)
    }
}
