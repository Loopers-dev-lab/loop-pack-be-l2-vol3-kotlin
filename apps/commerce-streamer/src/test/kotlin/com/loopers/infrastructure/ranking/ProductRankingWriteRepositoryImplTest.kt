package com.loopers.infrastructure.ranking

import com.loopers.config.ranking.RankingConfiguration
import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ProductRankingWriteRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Duration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import java.time.LocalDate

@SpringBootTest(classes = [ProductRankingWriteRepositoryImplTest.TestConfig::class])
@DisplayName("ProductRankingWriteRepositoryImpl")
class ProductRankingWriteRepositoryImplTest @Autowired constructor(
    private val productRankingWriteRepository: ProductRankingWriteRepository,
    private val redisConnectionFactory: RedisConnectionFactory,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
) {

    @AfterEach
    fun tearDown() {
        redisConnectionFactory.connection.use { it.serverCommands().flushAll() }
    }

    @Configuration
    @Import(RedisConfig::class, ProductRankingWriteRepositoryImpl::class, RankingConfiguration::class)
    class TestConfig

    companion object {
        @JvmStatic
        private val redisContainer = GenericContainer("redis:7.2-alpine").apply {
            withExposedPorts(6379)
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("datasource.redis.database") { 0 }
            registry.add("datasource.redis.master.host") { redisContainer.host }
            registry.add("datasource.redis.master.port") { redisContainer.getMappedPort(6379) }
            registry.add("datasource.redis.replicas[0].host") { redisContainer.host }
            registry.add("datasource.redis.replicas[0].port") { redisContainer.getMappedPort(6379) }
            registry.add("ranking.aggregation") { "daily" }
        }
    }

    @Test
    @DisplayName("yyyyMMdd 키 형식으로 점수를 적재하고 TTL을 설정한다")
    fun writesScoreWithFormattedKeyAndTtl() {
        val processingDate = LocalDate.of(2026, 4, 6)
        val key = "ranking:all:20260406"

        productRankingWriteRepository.incrementScore(processingDate, 101L, 0.7)

        assertThat(redisTemplate.opsForZSet().score(key, "101")).isEqualTo(0.7)
        val ttl = redisTemplate.getExpire(key)
        assertThat(ttl).isBetween(Duration.ofDays(2).seconds - 10, Duration.ofDays(2).seconds)
    }

    @Test
    @DisplayName("첫 번째 write에서 TTL을 설정하고, 이후 write에서는 TTL을 갱신하지 않는다")
    fun ttlNotRefreshedOnSecondWrite() {
        val processingDate = LocalDate.of(2026, 4, 6)
        val key = "ranking:all:20260406"

        productRankingWriteRepository.incrementScore(processingDate, 101L, 0.7)
        val ttl1 = redisTemplate.getExpire(key)

        // 약간의 대기 후 두 번째 write
        Thread.sleep(1000)
        productRankingWriteRepository.incrementScore(processingDate, 102L, 0.5)
        val ttl2 = redisTemplate.getExpire(key)

        // TTL이 갱신되지 않았으므로, ttl2는 ttl1보다 1초 적어야 함
        // (setTtlIfAbsent이므로 기존 TTL 유지)
        assertThat(ttl1 - ttl2).isGreaterThanOrEqualTo(0)
        assertThat(ttl1 - ttl2).isLessThanOrEqualTo(2)
    }

    @Test
    @DisplayName("음수 점수를 처리한다 (좋아요 취소)")
    fun writesNegativeScore() {
        val processingDate = LocalDate.of(2026, 4, 6)
        val key = "ranking:all:20260406"

        productRankingWriteRepository.incrementScore(processingDate, 101L, 0.2)
        productRankingWriteRepository.incrementScore(processingDate, 101L, -0.2)

        // 0.2 - 0.2 = 0.0
        val score = redisTemplate.opsForZSet().score(key, "101")
        assertThat(score).isEqualTo(0.0)
    }

    @Test
    @DisplayName("높은 점수 값을 처리한다 (다중 주문)")
    fun writesHighScore() {
        val processingDate = LocalDate.of(2026, 4, 6)
        val key = "ranking:all:20260406"
        val highScore = 700.0 // OrderScore: 0.7 * 1000 quantity

        productRankingWriteRepository.incrementScore(processingDate, 101L, highScore)

        assertThat(redisTemplate.opsForZSet().score(key, "101")).isEqualTo(highScore)
    }

    @Test
    @DisplayName("다중 상품에 대해 독립적으로 점수를 적재한다")
    fun writesIndependentScoresForMultipleProducts() {
        val processingDate = LocalDate.of(2026, 4, 6)
        val key = "ranking:all:20260406"

        productRankingWriteRepository.incrementScore(processingDate, 101L, 0.7)
        productRankingWriteRepository.incrementScore(processingDate, 102L, 0.5)
        productRankingWriteRepository.incrementScore(processingDate, 103L, 0.3)

        assertThat(redisTemplate.opsForZSet().score(key, "101")).isEqualTo(0.7)
        assertThat(redisTemplate.opsForZSet().score(key, "102")).isEqualTo(0.5)
        assertThat(redisTemplate.opsForZSet().score(key, "103")).isEqualTo(0.3)
    }
}
