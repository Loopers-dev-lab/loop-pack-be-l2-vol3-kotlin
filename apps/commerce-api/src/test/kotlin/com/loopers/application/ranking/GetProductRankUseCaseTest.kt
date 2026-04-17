package com.loopers.application.ranking

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.data.redis.RedisConnectionFailureException
import java.time.LocalDate

class GetProductRankUseCaseTest {

    private val rankingStore: RankingStore = mockk()
    private val weeklyRankReader: WeeklyRankReader = mockk {
        every { findLatestRankOfProduct(any()) } returns null
    }
    private val monthlyRankReader: MonthlyRankReader = mockk {
        every { findLatestRankOfProduct(any()) } returns null
    }
    private val useCase = GetProductRankUseCase(rankingStore, weeklyRankReader, monthlyRankReader)

    @DisplayName("상품 순위 조회")
    @Nested
    inner class Execute {

        @DisplayName("daily weekly monthly 순위가 모두 있으면 세 값을 함께 반환한다")
        @Test
        fun allRanksPresent() {
            every { rankingStore.getProductRank(any<LocalDate>(), 101L) } returns 0L
            every { weeklyRankReader.findLatestRankOfProduct(101L) } returns 5
            every { monthlyRankReader.findLatestRankOfProduct(101L) } returns 12

            val result = useCase.execute(101L)

            assertAll(
                { assertThat(result.daily).isEqualTo(1) },
                { assertThat(result.weekly).isEqualTo(5) },
                { assertThat(result.monthly).isEqualTo(12) },
            )
        }

        @DisplayName("랭킹에 없는 상품은 모든 순위가 null이다")
        @Test
        fun unrankedProduct() {
            every { rankingStore.getProductRank(any<LocalDate>(), 999L) } returns null

            val result = useCase.execute(999L)

            assertAll(
                { assertThat(result.daily).isNull() },
                { assertThat(result.weekly).isNull() },
                { assertThat(result.monthly).isNull() },
            )
        }

        @DisplayName("Redis 장애 시 daily가 null이 되어도 weekly monthly는 영향받지 않는다")
        @Test
        fun redisFailureDoesNotAffectMv() {
            every { rankingStore.getProductRank(any<LocalDate>(), any()) } throws
                RedisConnectionFailureException("Connection refused")
            every { weeklyRankReader.findLatestRankOfProduct(101L) } returns 7
            every { monthlyRankReader.findLatestRankOfProduct(101L) } returns 15

            val result = useCase.execute(101L)

            assertAll(
                { assertThat(result.daily).isNull() },
                { assertThat(result.weekly).isEqualTo(7) },
                { assertThat(result.monthly).isEqualTo(15) },
            )
        }
    }
}
