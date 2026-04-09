package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ProductRankingRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@DisplayName("RedisProductRankingRepository 통합 테스트")
@SpringBootTest(classes = [RedisProductRankingRepositoryIntegrationTest.TestApplication::class])
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "datasource.mysql-jpa.main.jdbc-url=jdbc:mysql://localhost:3306/loopers",
        "datasource.mysql-jpa.main.driver-class-name=com.mysql.cj.jdbc.Driver",
        "datasource.mysql-jpa.main.username=application",
        "datasource.mysql-jpa.main.password=application",
        "datasource.redis.master.host=localhost",
        "datasource.redis.master.port=6379",
        "datasource.redis.replicas[0].host=localhost",
        "datasource.redis.replicas[0].port=6380",
    ],
)
class RedisProductRankingRepositoryIntegrationTest
@Autowired
constructor(
    private val productRankingRepository: ProductRankingRepository,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    private fun todayKey(): String {
        val today = LocalDate.now(ZoneId.of("Asia/Seoul"))
        return "ranking:all:${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"
    }

    @Nested
    @DisplayName("날짜별 ZSET 키가 올바르게 생성되고 TTL이 설정된다")
    inner class KeyAndTtl {

        @Test
        @DisplayName("incrementScore 호출 시 ranking:all:yyyyMMdd 키가 생성된다")
        fun incrementScore_createsDateKey() {
            productRankingRepository.incrementScore(1L, 0.1)

            val key = todayKey()
            val exists = redisTemplate.hasKey(key)
            assertThat(exists).isTrue()
        }

        @Test
        @DisplayName("생성된 키에 TTL 2일이 설정된다")
        fun incrementScore_setsTtl() {
            productRankingRepository.incrementScore(1L, 0.1)

            val key = todayKey()
            val ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS)
            assertThat(ttl).isGreaterThan(172800 - 60)
            assertThat(ttl).isLessThanOrEqualTo(172800)
        }

        @Test
        @DisplayName("후속 increment 후에도 TTL이 유지된다")
        fun incrementScore_ttlPreservedAfterSubsequentCalls() {
            productRankingRepository.incrementScore(1L, 0.1)
            productRankingRepository.incrementScore(1L, 0.2)

            val key = todayKey()
            val ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS)
            assertThat(ttl).isGreaterThan(172800 - 60)
        }
    }

    @Nested
    @DisplayName("점수가 ZINCRBY로 누적된다")
    inner class ScoreAccumulation {

        @Test
        @DisplayName("같은 상품에 여러 번 increment → 점수 합산")
        fun incrementScore_accumulatesScore() {
            productRankingRepository.incrementScore(1L, 0.1)
            productRankingRepository.incrementScore(1L, 0.2)
            productRankingRepository.incrementScore(1L, 0.7)

            val key = todayKey()
            val score = redisTemplate.opsForZSet().score(key, "1")
            assertThat(score).isNotNull()
            assertThat(score!!).isCloseTo(1.0, Offset.offset(0.001))
        }

        @Test
        @DisplayName("다른 상품은 독립적으로 점수가 관리된다")
        fun incrementScore_independentPerProduct() {
            productRankingRepository.incrementScore(1L, 0.7)
            productRankingRepository.incrementScore(2L, 0.1)

            val key = todayKey()
            val score1 = redisTemplate.opsForZSet().score(key, "1")
            val score2 = redisTemplate.opsForZSet().score(key, "2")
            assertThat(score1!!).isCloseTo(0.7, Offset.offset(0.001))
            assertThat(score2!!).isCloseTo(0.1, Offset.offset(0.001))
        }
    }

    @Nested
    @DisplayName("가중치가 의도대로 랭킹 순서에 반영된다")
    inner class RankingOrder {

        @Test
        @DisplayName("주문 1건(0.7) 상품이 좋아요 3건(0.6) 상품보다 상위")
        fun incrementScore_orderBeatMultipleLikes() {
            productRankingRepository.incrementScore(1L, 0.7)

            productRankingRepository.incrementScore(2L, 0.2)
            productRankingRepository.incrementScore(2L, 0.2)
            productRankingRepository.incrementScore(2L, 0.2)

            val key = todayKey()
            val topRanked = redisTemplate.opsForZSet().reverseRange(key, 0, 0)
            assertThat(topRanked).containsExactly("1")
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ConfigurationPropertiesScan("com.loopers")
    @ComponentScan(
        basePackages = [
            "com.loopers.application",
            "com.loopers.domain",
            "com.loopers.config",
            "com.loopers.infrastructure",
            "com.loopers.interfaces",
            "com.loopers.support",
        ],
    )
    class TestApplication
}
