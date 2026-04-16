package com.loopers.batch.job.ranking.weekly

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("ProductAggregateDto")
class ProductAggregateDtoTest {

    @DisplayName("ProductAggregateDto를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("상품별 집계 데이터를 담는다.")
        @Test
        fun holdsAggregateData() {
            // act
            val dto = ProductAggregateDto(
                productId = 100L,
                viewCount = 1000L,
                likeCount = 200L,
                salesCount = 50L,
            )

            // assert
            assertThat(dto.productId).isEqualTo(100L)
            assertThat(dto.viewCount).isEqualTo(1000L)
            assertThat(dto.likeCount).isEqualTo(200L)
            assertThat(dto.salesCount).isEqualTo(50L)
        }
    }
}
