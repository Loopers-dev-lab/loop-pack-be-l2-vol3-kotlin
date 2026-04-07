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
            registry.add("ranking.strategy") { "daily" }
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
}
