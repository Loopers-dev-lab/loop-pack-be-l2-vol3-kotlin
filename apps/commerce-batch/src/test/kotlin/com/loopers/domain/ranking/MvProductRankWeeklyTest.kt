package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class MvProductRankWeeklyTest {

    @Test
    @DisplayName("MvProductRankWeekly 엔티티가 올바르게 생성된다")
    fun createsEntity() {
        // arrange & act
        val now = LocalDateTime.now()
        val entity = MvProductRankWeekly(
            productId = 1L,
            yearWeek = "2026-W15",
            rank = 1,
            score = 15.5,
            likeCount = 10L,
            orderCount = 5L,
            viewCount = 100L,
            version = 1,
            aggregatedAt = now,
        )

        // assert
        assertThat(entity.productId).isEqualTo(1L)
        assertThat(entity.yearWeek).isEqualTo("2026-W15")
        assertThat(entity.rank).isEqualTo(1)
        assertThat(entity.score).isEqualTo(15.5)
        assertThat(entity.likeCount).isEqualTo(10L)
        assertThat(entity.orderCount).isEqualTo(5L)
        assertThat(entity.viewCount).isEqualTo(100L)
        assertThat(entity.version).isEqualTo(1)
        assertThat(entity.aggregatedAt).isEqualTo(now)
    }

    @Test
    @DisplayName("MvProductRankMonthly 엔티티가 올바르게 생성된다")
    fun createsMonthlyEntity() {
        // arrange & act
        val now = LocalDateTime.now()
        val entity = MvProductRankMonthly(
            productId = 2L,
            yearMonth = "2026-04",
            rank = 3,
            score = 42.0,
            likeCount = 20L,
            orderCount = 8L,
            viewCount = 200L,
            version = 2,
            aggregatedAt = now,
        )

        // assert
        assertThat(entity.productId).isEqualTo(2L)
        assertThat(entity.yearMonth).isEqualTo("2026-04")
        assertThat(entity.rank).isEqualTo(3)
        assertThat(entity.version).isEqualTo(2)
    }
}
