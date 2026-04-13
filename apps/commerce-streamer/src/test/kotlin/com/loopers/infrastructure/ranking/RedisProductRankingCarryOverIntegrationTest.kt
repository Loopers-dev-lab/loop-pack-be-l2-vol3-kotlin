package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisConfig
import com.loopers.domain.ranking.ProductRankingRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
import java.util.concurrent.TimeUnit

@DisplayName("RedisProductRankingRepository carry-over 통합 테스트")
@SpringBootTest(classes = [RedisProductRankingCarryOverIntegrationTest.TestApplication::class])
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
class RedisProductRankingCarryOverIntegrationTest
@Autowired
constructor(
    private val productRankingRepository: ProductRankingRepository,
    @Qualifier(RedisConfig.REDIS_TEMPLATE_MASTER)
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisCleanUp: RedisCleanUp,
) {
    private val today = LocalDate.of(2026, 4, 10)
    private val tomorrow = LocalDate.of(2026, 4, 11)
    private val todayKey = RedisProductRankingRepository.buildKey(today)
    private val tomorrowKey = RedisProductRankingRepository.buildKey(tomorrow)

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("carry-over 실행 시 다음 날 키에 오늘 점수의 10%가 반영된다")
    inner class CarryOverScore {

        @BeforeEach
        fun setUp() {
            redisTemplate.opsForZSet().add(todayKey, "100", 10.0)
            redisTemplate.opsForZSet().add(todayKey, "101", 50.0)
        }

        @Test
        @DisplayName("오늘 10점 → 다음 날 1점 (10%)")
        fun carryOver_appliesWeight() {
            productRankingRepository.carryOver(today, tomorrow, 0.1)

            val score100 = redisTemplate.opsForZSet().score(tomorrowKey, "100")
            val score101 = redisTemplate.opsForZSet().score(tomorrowKey, "101")
            assertThat(score100!!).isCloseTo(1.0, Offset.offset(0.001))
            assertThat(score101!!).isCloseTo(5.0, Offset.offset(0.001))
        }
    }

    @Nested
    @DisplayName("carry-over 후 다음 날 키에 TTL 2일이 설정된다")
    inner class CarryOverTtl {

        @BeforeEach
        fun setUp() {
            redisTemplate.opsForZSet().add(todayKey, "100", 10.0)
        }

        @Test
        @DisplayName("TTL이 2일 범위 내로 설정된다")
        fun carryOver_setsTtl() {
            productRankingRepository.carryOver(today, tomorrow, 0.1)

            val ttl = redisTemplate.getExpire(tomorrowKey, TimeUnit.SECONDS)
            assertThat(ttl).isGreaterThan(172800 - 60)
            assertThat(ttl).isLessThanOrEqualTo(172800)
        }
    }

    @Nested
    @DisplayName("다음 날 키에 이벤트가 먼저 도착해 있으면 기존 점수에 합산된다")
    inner class MergeExisting {

        @BeforeEach
        fun setUp() {
            redisTemplate.opsForZSet().add(todayKey, "100", 10.0)
            redisTemplate.opsForZSet().add(tomorrowKey, "100", 3.0)
        }

        @Test
        @DisplayName("기존 3점 + carry-over 1점 = 4점")
        fun carryOver_mergesWithExisting() {
            productRankingRepository.carryOver(today, tomorrow, 0.1)

            val score = redisTemplate.opsForZSet().score(tomorrowKey, "100")
            assertThat(score!!).isCloseTo(4.0, Offset.offset(0.001))
        }

        @Test
        @DisplayName("ZUNIONSTORE 후에도 TTL 2일이 재설정된다 (dest 존재 분기)")
        fun carryOver_mergeBranch_preservesTtl() {
            productRankingRepository.carryOver(today, tomorrow, 0.1)

            val ttl = redisTemplate.getExpire(tomorrowKey, TimeUnit.SECONDS)
            assertThat(ttl).isGreaterThan(172800 - 60)
            assertThat(ttl).isLessThanOrEqualTo(172800)
        }
    }

    @Nested
    @DisplayName("carry-over 후 오늘 키의 점수는 변경되지 않는다")
    inner class SourcePreserved {

        @BeforeEach
        fun setUp() {
            redisTemplate.opsForZSet().add(todayKey, "100", 10.0)
        }

        @Test
        @DisplayName("오늘 키 점수 그대로 유지")
        fun carryOver_preservesSource() {
            productRankingRepository.carryOver(today, tomorrow, 0.1)

            val score = redisTemplate.opsForZSet().score(todayKey, "100")
            assertThat(score!!).isCloseTo(10.0, Offset.offset(0.001))
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
