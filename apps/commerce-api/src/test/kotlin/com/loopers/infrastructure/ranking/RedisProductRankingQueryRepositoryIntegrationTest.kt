package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ProductRankingQueryRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate

@DisplayName("RedisProductRankingQueryRepository 통합 테스트")
@SpringBootTest
class RedisProductRankingQueryRepositoryIntegrationTest
@Autowired
constructor(
    private val productRankingQueryRepository: ProductRankingQueryRepository,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisCleanUp: RedisCleanUp,
) {
    private val date = LocalDate.of(2026, 4, 10)
    private val key = RedisProductRankingQueryRepository.buildKey(date)

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @BeforeEach
    fun setUp() {
        redisTemplate.opsForZSet().add(key, "100", 10.5)
        redisTemplate.opsForZSet().add(key, "101", 8.3)
        redisTemplate.opsForZSet().add(key, "102", 5.0)
        redisTemplate.opsForZSet().add(key, "103", 3.2)
        redisTemplate.opsForZSet().add(key, "104", 1.0)
    }

    @Nested
    @DisplayName("getTopRanked — ZREVRANGE WITHSCORES 조회")
    inner class GetTopRanked {

        @Test
        @DisplayName("offset=0, count=3 → 1~3위 반환, 점수 내림차순")
        fun getTopRanked_firstPage() {
            val result = productRankingQueryRepository.getTopRanked(date, 0, 3)

            assertThat(result).hasSize(3)
            assertThat(result[0].productId).isEqualTo(100)
            assertThat(result[0].rank).isEqualTo(1)
            assertThat(result[0].score).isEqualTo(10.5)
            assertThat(result[1].productId).isEqualTo(101)
            assertThat(result[1].rank).isEqualTo(2)
            assertThat(result[2].productId).isEqualTo(102)
            assertThat(result[2].rank).isEqualTo(3)
        }

        @Test
        @DisplayName("offset=3, count=3 → 4~5위 반환 (짧은 페이지)")
        fun getTopRanked_secondPage() {
            val result = productRankingQueryRepository.getTopRanked(date, 3, 3)

            assertThat(result).hasSize(2)
            assertThat(result[0].productId).isEqualTo(103)
            assertThat(result[0].rank).isEqualTo(4)
            assertThat(result[1].productId).isEqualTo(104)
            assertThat(result[1].rank).isEqualTo(5)
        }

        @Test
        @DisplayName("데이터 없는 날짜 → 빈 리스트")
        fun getTopRanked_emptyDate() {
            val result = productRankingQueryRepository.getTopRanked(
                LocalDate.of(2099, 1, 1),
                0,
                10,
            )

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getRank — ZREVRANK 조회")
    inner class GetRank {

        @Test
        @DisplayName("존재하는 상품 → 1-based 순위 반환")
        fun getRank_exists() {
            val rank = productRankingQueryRepository.getRank(date, 101)

            assertThat(rank).isEqualTo(2)
        }

        @Test
        @DisplayName("존재하지 않는 상품 → null")
        fun getRank_notExists() {
            val rank = productRankingQueryRepository.getRank(date, 999)

            assertThat(rank).isNull()
        }

        @Test
        @DisplayName("키가 없는 날짜 → null")
        fun getRank_noKey() {
            val rank = productRankingQueryRepository.getRank(LocalDate.of(2099, 1, 1), 100)

            assertThat(rank).isNull()
        }
    }

    @Nested
    @DisplayName("getTotalCount — ZCARD 조회")
    inner class GetTotalCount {

        @Test
        @DisplayName("상품 5개 → 5 반환")
        fun getTotalCount_returns5() {
            val count = productRankingQueryRepository.getTotalCount(date)

            assertThat(count).isEqualTo(5)
        }

        @Test
        @DisplayName("키가 없는 날짜 → 0 반환")
        fun getTotalCount_noKey() {
            val count = productRankingQueryRepository.getTotalCount(LocalDate.of(2099, 1, 1))

            assertThat(count).isEqualTo(0)
        }
    }
}
