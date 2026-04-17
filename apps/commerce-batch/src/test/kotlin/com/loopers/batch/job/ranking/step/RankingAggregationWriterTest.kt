package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.ProductRankMonthlyRepository
import com.loopers.domain.ranking.ProductRankResult
import com.loopers.domain.ranking.ProductRankWeeklyRepository
import com.loopers.domain.ranking.RankingPeriodType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.batch.item.Chunk
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class RankingAggregationWriterTest {

    @Mock
    private lateinit var weeklyRepository: ProductRankWeeklyRepository

    @Mock
    private lateinit var monthlyRepository: ProductRankMonthlyRepository

    @Nested
    @DisplayName("periodType별 올바른 리포지토리 호출")
    inner class RepositoryRouting {

        @DisplayName("WEEKLY일 때 weeklyRepository.batchInsert를 호출한다")
        @Test
        fun write_whenWeekly_callsWeeklyRepository() {
            // arrange
            val requestDate = LocalDate.of(2026, 4, 15)
            val writer = RankingAggregationWriter(RankingPeriodType.WEEKLY, requestDate, weeklyRepository, monthlyRepository)
            val chunk = Chunk(listOf(createResult(1L, 1), createResult(2L, 2)))

            // act
            writer.write(chunk)

            // assert
            verify(weeklyRepository).batchInsert(
                any(),
                eq(LocalDate.of(2026, 4, 13)),
                eq(LocalDate.of(2026, 4, 19)),
            )
            verifyNoInteractions(monthlyRepository)
        }

        @DisplayName("MONTHLY일 때 monthlyRepository.batchInsert를 호출한다")
        @Test
        fun write_whenMonthly_callsMonthlyRepository() {
            // arrange
            val requestDate = LocalDate.of(2026, 4, 15)
            val writer = RankingAggregationWriter(RankingPeriodType.MONTHLY, requestDate, weeklyRepository, monthlyRepository)
            val chunk = Chunk(listOf(createResult(1L, 1)))

            // act
            writer.write(chunk)

            // assert
            verify(monthlyRepository).batchInsert(
                any(),
                eq(LocalDate.of(2026, 4, 1)),
                eq(LocalDate.of(2026, 4, 30)),
            )
            verifyNoInteractions(weeklyRepository)
        }
    }

    private fun createResult(productId: Long, rank: Int): ProductRankResult =
        ProductRankResult(
            productId = productId,
            totalScore = 100.0 - rank,
            viewCount = 10,
            likeCount = 5,
            orderCount = 3,
            rank = rank,
        )
}
