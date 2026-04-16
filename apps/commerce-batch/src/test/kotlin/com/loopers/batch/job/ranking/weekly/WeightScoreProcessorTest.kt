package com.loopers.batch.job.ranking.weekly

import com.loopers.batch.job.ranking.ProductAggregateDto
import com.loopers.batch.job.ranking.RankingWeightProperties
import com.loopers.batch.job.ranking.WeightScoreProcessor
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("WeightScoreProcessor")
class WeightScoreProcessorTest {

    @DisplayName("가중치 계산 시,")
    @Nested
    inner class Process {

        @DisplayName("view×0.1 + like×0.2 + sales×0.7 공식으로 score를 계산한다.")
        @Test
        fun calculatesWeightedScore() {
            // arrange
            val properties = RankingWeightProperties(
                viewWeight = 0.1,
                likeWeight = 0.2,
                salesWeight = 0.7,
            )
            val processor = WeightScoreProcessor(properties, "2026-W16")
            val input = ProductAggregateDto(
                productId = 100L,
                viewCount = 1000L,
                likeCount = 200L,
                salesCount = 50L,
            )

            // act
            val result = processor.process(input)

            // assert — 1000×0.1 + 200×0.2 + 50×0.7 = 100 + 40 + 35 = 175.0
            assertThat(result).isNotNull
            assertThat(result!!.productId).isEqualTo(100L)
            assertThat(result.score).isCloseTo(175.0, Offset.offset(0.001))
            assertThat(result.periodKey).isEqualTo("2026-W16")
            assertThat(result.viewCount).isEqualTo(1000L)
            assertThat(result.likeCount).isEqualTo(200L)
            assertThat(result.salesCount).isEqualTo(50L)
        }

        @DisplayName("모든 집계가 0이면 score는 0.0이다.")
        @Test
        fun zeroCountsReturnZeroScore() {
            // arrange
            val properties = RankingWeightProperties(0.1, 0.2, 0.7)
            val processor = WeightScoreProcessor(properties, "2026-W16")
            val input = ProductAggregateDto(100L, 0L, 0L, 0L)

            // act
            val result = processor.process(input)

            // assert
            assertThat(result).isNotNull
            assertThat(result!!.score).isEqualTo(0.0)
        }
    }
}
