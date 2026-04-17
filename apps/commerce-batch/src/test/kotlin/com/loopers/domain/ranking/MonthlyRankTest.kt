package com.loopers.domain.ranking

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class MonthlyRankTest {

    @Test
    fun `create는 yearMonth를 복합키에, 순위-점수-수치를 본문에 세팅한다`() {
        val rank = MonthlyRank.create(
            productId = 9L,
            yearMonth = "202604",
            rankPosition = 3,
            totalScore = 888.8,
            viewCount = 3000,
            likeCount = 800,
            orderCount = 50,
        )

        assertAll(
            { assertThat(rank.productId).isEqualTo(9L) },
            { assertThat(rank.yearMonth).isEqualTo("202604") },
            { assertThat(rank.rankPosition).isEqualTo(3) },
            { assertThat(rank.totalScore).isEqualTo(888.8) },
            { assertThat(rank.viewCount).isEqualTo(3000L) },
            { assertThat(rank.likeCount).isEqualTo(800L) },
            { assertThat(rank.orderCount).isEqualTo(50L) },
        )
    }
}
