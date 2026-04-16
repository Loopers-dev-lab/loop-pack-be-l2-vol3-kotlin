package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate

@SpringBootTest(classes = [RedisTestContainersConfig::class, RedisConfig::class, RedisCleanUp::class, RedisRankingStoreImpl::class])
@DisplayName("RedisRankingStoreImpl 통합 테스트")
class RedisRankingStoreIntegrationTest @Autowired constructor(
    private val rankingStore: RedisRankingStoreImpl,
    private val redisCleanUp: RedisCleanUp,
    private val redisTemplate: RedisTemplate<String, String>,
) {
    private val testKey = "ranking:all:20250907"

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @BeforeEach
    fun setUp() {
        redisTemplate.opsForZSet().add(testKey, "101", 100.0)
        redisTemplate.opsForZSet().add(testKey, "202", 50.0)
        redisTemplate.opsForZSet().add(testKey, "303", 75.0)
    }

    @Nested
    @DisplayName("getTopProducts")
    inner class GetTopProducts {

        @Test
        @DisplayName("score 내림차순으로 Top-N을 반환한다")
        fun `Top-N 조회`() {
            val result = rankingStore.getTopProducts(testKey, 0, 3)

            assertThat(result).hasSize(3)
            assertThat(result[0].productId).isEqualTo(101L)
            assertThat(result[0].score).isEqualTo(100.0)
            assertThat(result[1].productId).isEqualTo(303L)
            assertThat(result[2].productId).isEqualTo(202L)
        }

        @Test
        @DisplayName("offset으로 페이징할 수 있다")
        fun `offset 페이징`() {
            val result = rankingStore.getTopProducts(testKey, 1, 2)

            assertThat(result).hasSize(2)
            assertThat(result[0].productId).isEqualTo(303L)
            assertThat(result[1].productId).isEqualTo(202L)
        }

        @Test
        @DisplayName("존재하지 않는 키는 빈 리스트 반환")
        fun `존재하지 않는 키`() {
            val result = rankingStore.getTopProducts("ranking:all:99991231", 0, 10)

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getTotalCount")
    inner class GetTotalCount {

        @Test
        @DisplayName("ZSET의 멤버 수를 반환한다")
        fun `멤버 수 조회`() {
            val count = rankingStore.getTotalCount(testKey)

            assertThat(count).isEqualTo(3)
        }

        @Test
        @DisplayName("존재하지 않는 키는 0 반환")
        fun `존재하지 않는 키`() {
            val count = rankingStore.getTotalCount("ranking:all:99991231")

            assertThat(count).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("getRank")
    inner class GetRank {

        @Test
        @DisplayName("score 내림차순 기준 0-based 순위를 반환한다")
        fun `순위 조회`() {
            assertThat(rankingStore.getRank(testKey, 101L)).isEqualTo(0)
            assertThat(rankingStore.getRank(testKey, 303L)).isEqualTo(1)
            assertThat(rankingStore.getRank(testKey, 202L)).isEqualTo(2)
        }

        @Test
        @DisplayName("존재하지 않는 멤버는 null 반환")
        fun `없는 멤버`() {
            assertThat(rankingStore.getRank(testKey, 999L)).isNull()
        }
    }

    @Nested
    @DisplayName("getScore")
    inner class GetScore {

        @Test
        @DisplayName("멤버의 score를 반환한다")
        fun `score 조회`() {
            assertThat(rankingStore.getScore(testKey, 101L)).isEqualTo(100.0)
        }

        @Test
        @DisplayName("존재하지 않는 멤버는 null 반환")
        fun `없는 멤버`() {
            assertThat(rankingStore.getScore(testKey, 999L)).isNull()
        }
    }

    @Nested
    @DisplayName("가중치 기반 순위 검증")
    inner class WeightedRanking {

        @Test
        @DisplayName("ORDER 1건(weight=0.7)이 LIKE 3건(weight=0.2*3=0.6)보다 순위가 높다")
        fun `가중치 순위 검증`() {
            val key = "ranking:all:20250908"
            // 상품 A: ORDER 1건 = 0.7 * log10(10000+1) ≈ 2.8
            redisTemplate.opsForZSet().add(key, "1", 2.8)
            // 상품 B: LIKE 3건 = 0.2 * 3 = 0.6
            redisTemplate.opsForZSet().add(key, "2", 0.6)

            val result = rankingStore.getTopProducts(key, 0, 2)

            assertThat(result[0].productId).isEqualTo(1L)
            assertThat(result[1].productId).isEqualTo(2L)
        }
    }
}
