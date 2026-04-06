package com.loopers.domain.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.RedisTemplate
import java.time.LocalDate

@SpringBootTest
@Import(RedisTestContainersConfig::class)
@DisplayName("ProductRankingRepository Redis 테스트")
class ProductRankingRepositoryTest @Autowired constructor(
    private val productRankingRepository: ProductRankingRepository,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {

    companion object {
        private const val RANKING_KEY_PREFIX = "ranking:all:"
    }

    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("Redis 정렬 순서를 유지하면서 1-based rank를 반환한다")
    fun returnsRankedProductsInRedisOrder() {
        val processingDate = LocalDate.of(2026, 4, 6)
        val key = "${RANKING_KEY_PREFIX}20260406"

        redisTemplate.opsForZSet().add(key, "101", 10.0)
        redisTemplate.opsForZSet().add(key, "102", 30.0)
        redisTemplate.opsForZSet().add(key, "103", 20.0)

        val rankedProducts = productRankingRepository.getRankedProducts(processingDate, page = 0, size = 3)

        assertThat(rankedProducts.map { it.productId }).containsExactly(102L, 103L, 101L)
        assertThat(rankedProducts.map { it.rank }).containsExactly(1L, 2L, 3L)
    }

    @Test
    @DisplayName("페이지 offset을 반영해 rank를 이어서 계산한다")
    fun calculatesRankFromZeroBasedPageOffset() {
        val processingDate = LocalDate.of(2026, 4, 6)
        val key = "${RANKING_KEY_PREFIX}20260406"

        (1..5).forEach { index ->
            redisTemplate.opsForZSet().add(key, "20$index", index.toDouble())
        }

        val rankedProducts = productRankingRepository.getRankedProducts(processingDate, page = 1, size = 2)

        assertThat(rankedProducts.map { it.productId }).containsExactly(203L, 202L)
        assertThat(rankedProducts.map { it.rank }).containsExactly(3L, 4L)
    }
}
