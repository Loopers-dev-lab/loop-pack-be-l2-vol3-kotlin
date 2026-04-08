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

    @Test
    @DisplayName("빈 Redis key (데이터 없음)는 empty list를 반환한다")
    fun emptyRedisKeyReturnsEmptyList() {
        val processingDate = LocalDate.of(2026, 4, 6)

        val rankedProducts = productRankingRepository.getRankedProducts(processingDate, page = 0, size = 20)

        assertThat(rankedProducts).isEmpty()
    }

    @Test
    @DisplayName("count() 메서드는 ZSET의 총 카운트를 반환한다")
    fun countReturnsCorrectly() {
        val processingDate = LocalDate.of(2026, 4, 6)
        val key = "${RANKING_KEY_PREFIX}20260406"

        (1..10).forEach { index ->
            redisTemplate.opsForZSet().add(key, index.toString(), index.toDouble())
        }

        val count = productRankingRepository.count(processingDate)

        assertThat(count).isEqualTo(10L)
    }

    @Test
    @DisplayName("count() 메서드는 빈 ZSET에 대해 0을 반환한다")
    fun countReturnsZeroForEmptySet() {
        val processingDate = LocalDate.of(2026, 4, 6)

        val count = productRankingRepository.count(processingDate)

        assertThat(count).isEqualTo(0L)
    }

    @Test
    @DisplayName("getRankedProductsWithCount()는 products와 count를 동시에 반환한다")
    fun getRankedProductsWithCountReturnsCorrectly() {
        val processingDate = LocalDate.of(2026, 4, 6)
        val key = "${RANKING_KEY_PREFIX}20260406"

        (1..5).forEach { index ->
            redisTemplate.opsForZSet().add(key, index.toString(), index.toDouble())
        }

        val result = productRankingRepository.getRankedProductsWithCount(processingDate, page = 0, size = 2)

        assertThat(result.products).hasSize(2)
        assertThat(result.count).isEqualTo(5L)
        assertThat(result.products.map { it.rank }).containsExactly(1L, 2L)
    }

    @Test
    @DisplayName("getRankedProductsWithCount()는 빈 ZSET에 대해 empty products와 0 count를 반환한다")
    fun getRankedProductsWithCountReturnsEmptyForNoData() {
        val processingDate = LocalDate.of(2026, 4, 6)

        val result = productRankingRepository.getRankedProductsWithCount(processingDate, page = 0, size = 20)

        assertThat(result.products).isEmpty()
        assertThat(result.count).isEqualTo(0L)
    }
}
