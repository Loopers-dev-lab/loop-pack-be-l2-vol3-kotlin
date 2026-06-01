package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.ProductRankResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.batch.item.Chunk
import java.time.LocalDate

class RankingAggregationWriterTest {

    @Nested
    @DisplayName("MV 테이블 적재")
    inner class WriteMv {

        @DisplayName("chunk의 아이템을 batchInsertAction으로 전달한다")
        @Test
        fun write_callsBatchInsertWithCorrectParams() {
            // arrange
            val captured = mutableListOf<Triple<List<ProductRankResult>, LocalDate, LocalDate>>()
            val startDate = LocalDate.of(2026, 4, 13)
            val endDate = LocalDate.of(2026, 4, 19)

            val writer = RankingAggregationWriter(
                batchInsertAction = { items, start, end -> captured.add(Triple(items, start, end)) },
                periodStartDate = startDate,
                periodEndDate = endDate,
            )
            val chunk = Chunk(listOf(createResult(1L, 1), createResult(2L, 2)))

            // act
            writer.write(chunk)

            // assert
            assertThat(captured).hasSize(1)
            assertThat(captured[0].first).hasSize(2)
            assertThat(captured[0].second).isEqualTo(startDate)
            assertThat(captured[0].third).isEqualTo(endDate)
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
