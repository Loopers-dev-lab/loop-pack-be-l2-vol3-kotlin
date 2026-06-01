package com.loopers.batch.job.ranking.step

import com.loopers.domain.ranking.ProductRankAggregation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class RankingAggregationProcessorTest {

    @Nested
    @DisplayName("순위 부여")
    inner class RankAssignment {

        @DisplayName("순차적으로 rank를 1부터 부여한다")
        @Test
        fun assignRank_sequentially() {
            // arrange
            val processor = RankingAggregationProcessor()
            val items = listOf(
                ProductRankAggregation(productId = 1L, totalScore = 100.0, viewCount = 50, likeCount = 30, orderCount = 20),
                ProductRankAggregation(productId = 2L, totalScore = 80.0, viewCount = 40, likeCount = 25, orderCount = 15),
                ProductRankAggregation(productId = 3L, totalScore = 60.0, viewCount = 30, likeCount = 20, orderCount = 10),
            )

            // act
            val results = items.map { processor.process(it) }

            // assert
            assertAll(
                { assertThat(results[0]!!.rank).isEqualTo(1) },
                { assertThat(results[1]!!.rank).isEqualTo(2) },
                { assertThat(results[2]!!.rank).isEqualTo(3) },
            )
        }

        @DisplayName("원본 데이터가 결과에 올바르게 매핑된다")
        @Test
        fun mapFields_correctly() {
            // arrange
            val processor = RankingAggregationProcessor()
            val item = ProductRankAggregation(
                productId = 42L,
                totalScore = 95.5,
                viewCount = 100,
                likeCount = 50,
                orderCount = 25,
            )

            // act
            val result = processor.process(item)!!

            // assert
            assertAll(
                { assertThat(result.productId).isEqualTo(42L) },
                { assertThat(result.totalScore).isEqualTo(95.5) },
                { assertThat(result.viewCount).isEqualTo(100) },
                { assertThat(result.likeCount).isEqualTo(50) },
                { assertThat(result.orderCount).isEqualTo(25) },
                { assertThat(result.rank).isEqualTo(1) },
            )
        }
    }
}
