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
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate

@SpringBootTest
@Import(RedisTestContainersConfig::class)
@TestPropertySource(
    properties = [
        "ranking.aggregation=sliding-window",
        "ranking.sliding-window.window-days=3",
        "ranking.sliding-window.decay-factor=0.5",
    ],
)
@DisplayName("ProductRankingRepository 슬라이딩 윈도우 테스트")
class ProductRankingRepositorySlidingWindowTest @Autowired constructor(
    private val productRankingRepository: ProductRankingRepository,
    private val redisCleanUp: RedisCleanUp,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {

    companion object {
        private const val KEY_PREFIX = "ranking:all:"
        private val ANCHOR_DATE: LocalDate = LocalDate.of(2026, 4, 6)
    }

    @BeforeEach
    fun setUp() {
        redisCleanUp.truncateAll()
    }

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    /**
     * windowDays=3, decayFactor=0.5 설정 기준:
     * - today    (daysAgo=0): weight = 0.5^0 = 1.0
     * - yesterday(daysAgo=1): weight = 0.5^1 = 0.5
     * - 2일전    (daysAgo=2): weight = 0.5^2 = 0.25
     * - 3일전    (daysAgo=3): weight = 0.5^3 = 0.125
     */
    @Test
    @DisplayName("여러 날짜 점수가 decay weight와 함께 합산되어 랭킹이 결정된다")
    fun slidingWindowMergesMultiDateScoresWithDecay() {
        // product1: 오늘 score=10  → merged = 10 × 1.0 = 10.0
        // product2: 어제 score=16  → merged = 16 × 0.5 = 8.0
        // → product1 rank=1, product2 rank=2
        redisTemplate.opsForZSet().add("${KEY_PREFIX}20260406", "1", 10.0)
        redisTemplate.opsForZSet().add("${KEY_PREFIX}20260405", "2", 16.0)

        val result = productRankingRepository.getRankedProductsWithCount(ANCHOR_DATE, page = 0, size = 10)

        assertThat(result.count).isEqualTo(2L)
        assertThat(result.products.map { it.productId }).containsExactly(1L, 2L)
        assertThat(result.products.map { it.rank }).containsExactly(1L, 2L)
    }

    @Test
    @DisplayName("오래된 높은 점수보다 최근의 낮은 점수가 더 높은 가중치를 받는다")
    fun recentLowScoreOutweighsOldHighScore() {
        // product1: 3일전 score=100 → merged = 100 × 0.125 = 12.5
        // product2: 오늘  score=20  → merged = 20  × 1.0   = 20.0
        // → product2 rank=1 (최신 데이터 우대)
        redisTemplate.opsForZSet().add("${KEY_PREFIX}20260403", "1", 100.0)
        redisTemplate.opsForZSet().add("${KEY_PREFIX}20260406", "2", 20.0)

        val result = productRankingRepository.getRankedProductsWithCount(ANCHOR_DATE, page = 0, size = 10)

        assertThat(result.products.map { it.productId }).containsExactly(2L, 1L)
    }

    @Test
    @DisplayName("데이터가 없을 때 슬라이딩 윈도우는 빈 결과를 반환한다")
    fun emptyDataReturnsEmptyResult() {
        val result = productRankingRepository.getRankedProductsWithCount(ANCHOR_DATE, page = 0, size = 20)

        assertThat(result.products).isEmpty()
        assertThat(result.count).isEqualTo(0L)
    }

    @Test
    @DisplayName("조회 완료 후 임시 key(ranking:tmp:*)가 Redis에 남지 않는다")
    fun tempKeyIsCleanedUpAfterRead() {
        redisTemplate.opsForZSet().add("${KEY_PREFIX}20260406", "1", 10.0)

        productRankingRepository.getRankedProductsWithCount(ANCHOR_DATE, page = 0, size = 10)

        val tempKeys = redisTemplate.keys("ranking:tmp:*")
        assertThat(tempKeys).isEmpty()
    }

    @Test
    @DisplayName("윈도우 범위 밖의 날짜 데이터는 합산에 포함되지 않는다")
    fun dataOutsideWindowIsExcluded() {
        // windowDays=3이므로 4일전 데이터는 포함되지 않음
        redisTemplate.opsForZSet().add("${KEY_PREFIX}20260402", "1", 999.0) // 4일전 (범위 밖)
        redisTemplate.opsForZSet().add("${KEY_PREFIX}20260406", "2", 1.0) // 오늘 (범위 안)

        val result = productRankingRepository.getRankedProductsWithCount(ANCHOR_DATE, page = 0, size = 10)

        // product1(999점 but 4일전)은 포함되지 않음, product2만 반환
        assertThat(result.products.map { it.productId }).containsExactly(2L)
    }
}
