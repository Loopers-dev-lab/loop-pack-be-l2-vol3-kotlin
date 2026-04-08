package com.loopers.application.ranking

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.redis.RedisConnectionFailureException
import java.time.LocalDate

class GetProductRankUseCaseTest {

    private val rankingStore: RankingStore = mockk()
    private val useCase = GetProductRankUseCase(rankingStore)

    @DisplayName("상품 순위 조회")
    @Nested
    inner class Execute {

        @DisplayName("랭킹에 있는 상품은 1-based 순위를 반환한다")
        @Test
        fun rankedProduct() {
            every { rankingStore.getProductRank(any<LocalDate>(), 101L) } returns 0L

            val result = useCase.execute(101L)

            assertThat(result).isEqualTo(1)
        }

        @DisplayName("랭킹에 없는 상품은 null을 반환한다")
        @Test
        fun unrankedProduct() {
            every { rankingStore.getProductRank(any<LocalDate>(), 999L) } returns null

            val result = useCase.execute(999L)

            assertThat(result).isNull()
        }

        @DisplayName("Redis 장애 시 null을 반환한다")
        @Test
        fun redisFailureReturnsNull() {
            every { rankingStore.getProductRank(any<LocalDate>(), any()) } throws
                RedisConnectionFailureException("Connection refused")

            val result = useCase.execute(101L)

            assertThat(result).isNull()
        }
    }
}
