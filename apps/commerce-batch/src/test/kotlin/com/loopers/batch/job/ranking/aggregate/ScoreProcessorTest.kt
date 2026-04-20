package com.loopers.batch.job.ranking.aggregate

import com.loopers.batch.job.ranking.aggregate.step.ScoreProcessor
import kotlin.math.log10
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ScoreProcessor 단위 테스트")
class ScoreProcessorTest {

    private val processor = ScoreProcessor()

    @Test
    @DisplayName("점수 공식 viewCount*0.1 + likeCount*0.2 + 0.7*log10(orderAmountSum+1)이 올바르게 적용된다")
    fun shouldCalculateScoreCorrectly() {
        // arrange
        val input = ProductMetricRow(
            productId = 1L,
            viewCount = 1000L,
            likeCount = 500L,
            orderCount = 100L,
            orderAmountSum = 5000000L,
        )
        val expectedScore = 1000 * 0.1 + 500 * 0.2 + 0.7 * log10(5000000.0 + 1.0)

        // act
        val result = processor.process(input)!!

        // assert
        assertThat(result.productId).isEqualTo(1L)
        assertThat(result.score).isCloseTo(expectedScore, Offset.offset(0.0001))
        assertThat(result.viewCount).isEqualTo(1000L)
        assertThat(result.likeCount).isEqualTo(500L)
        assertThat(result.orderCount).isEqualTo(100L)
        assertThat(result.orderAmountSum).isEqualTo(5000000L)
    }

    @Test
    @DisplayName("orderAmountSum이 0이면 log10(1)=0이 적용된다")
    fun shouldHandleZeroOrderAmountSum() {
        // arrange
        val input = ProductMetricRow(
            productId = 2L,
            viewCount = 100L,
            likeCount = 50L,
            orderCount = 0L,
            orderAmountSum = 0L,
        )
        val expectedScore = 100 * 0.1 + 50 * 0.2 + 0.7 * log10(1.0)

        // act
        val result = processor.process(input)!!

        // assert
        assertThat(result.score).isCloseTo(expectedScore, Offset.offset(0.0001))
    }

    @Test
    @DisplayName("모든 지표가 0이면 점수도 0이다")
    fun shouldReturnZeroScoreWhenAllMetricsAreZero() {
        // arrange
        val input = ProductMetricRow(
            productId = 3L,
            viewCount = 0L,
            likeCount = 0L,
            orderCount = 0L,
            orderAmountSum = 0L,
        )

        // act
        val result = processor.process(input)!!

        // assert
        assertThat(result.score).isCloseTo(0.0, Offset.offset(0.0001))
    }
}
