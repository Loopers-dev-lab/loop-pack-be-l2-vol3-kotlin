package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("MvProductRankWeekly")
class MvProductRankWeeklyTest {

    @DisplayName("MvProductRankWeekly를 생성할 때,")
    @Nested
    inner class Create {

        @DisplayName("yearWeek와 productId로 복합키가 구성되고 랭킹 정보가 설정된다.")
        @Test
        fun createsWithCompositeKey() {
            // arrange & act
            val entity = MvProductRankWeekly(
                yearWeek = "2026-W16",
                productId = 100L,
                rankNum = 1,
                score = 95.5,
                viewCount = 1000L,
                likeCount = 200L,
                salesCount = 50L,
                updatedAt = LocalDateTime.of(2026, 4, 16, 3, 0),
            )

            // assert
            assertThat(entity.yearWeek).isEqualTo("2026-W16")
            assertThat(entity.productId).isEqualTo(100L)
            assertThat(entity.rankNum).isEqualTo(1)
            assertThat(entity.score).isEqualTo(95.5)
            assertThat(entity.viewCount).isEqualTo(1000L)
            assertThat(entity.likeCount).isEqualTo(200L)
            assertThat(entity.salesCount).isEqualTo(50L)
        }
    }
}
