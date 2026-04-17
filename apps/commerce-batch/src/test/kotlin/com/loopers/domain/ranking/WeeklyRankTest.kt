package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import java.time.LocalDate

class WeeklyRankTest {

    @Test
    fun `create는 weekEnd를 복합키에, weekStart와 순위-점수-수치를 본문에 세팅한다`() {
        val rank = WeeklyRank.create(
            productId = 7L,
            weekStart = LocalDate.of(2026, 4, 13),
            weekEnd = LocalDate.of(2026, 4, 19),
            rankPosition = 1,
            totalScore = 123.4,
            viewCount = 500,
            likeCount = 100,
            orderCount = 20,
        )

        assertAll(
            { assertThat(rank.productId).isEqualTo(7L) },
            { assertThat(rank.weekEnd).isEqualTo(LocalDate.of(2026, 4, 19)) },
            { assertThat(rank.weekStart).isEqualTo(LocalDate.of(2026, 4, 13)) },
            { assertThat(rank.rankPosition).isEqualTo(1) },
            { assertThat(rank.totalScore).isEqualTo(123.4) },
            { assertThat(rank.viewCount).isEqualTo(500L) },
            { assertThat(rank.likeCount).isEqualTo(100L) },
            { assertThat(rank.orderCount).isEqualTo(20L) },
        )
    }
}
