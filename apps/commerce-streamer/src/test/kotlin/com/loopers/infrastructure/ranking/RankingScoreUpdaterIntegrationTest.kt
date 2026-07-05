package com.loopers.infrastructure.ranking

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Kafka 컨텍스트 없이 Redis만 띄워 랭킹 적재 로직을 검증한다.
 * Spring 컨텍스트를 부트하면 KafkaListener가 브로커 연결을 시도하므로
 * RedisTemplate을 수동 조립해 대상 컴포넌트만 테스트한다.
 */
class RankingScoreUpdaterIntegrationTest {

    private lateinit var rankingScoreUpdater: RankingScoreUpdater

    @BeforeEach
    fun setUp() {
        redisTemplate.connectionFactory!!.connection.use { it.serverCommands().flushAll() }
        // TTL 설정 캐시가 테스트 간 공유되지 않도록 매번 새로 만든다.
        rankingScoreUpdater = RankingScoreUpdater(redisTemplate)
    }

    @Nested
    inner class ScoreAccumulation {

        @Test
        fun `조회 이벤트는 VIEW 가중치가 곱해져 반영된다`() {
            rankingScoreUpdater.incrementScore(1L, RankingEventType.VIEW, DATE)

            val score = redisTemplate.opsForZSet().score(KEY, "1")

            assertThat(score!!).isCloseTo(0.1, within(TOLERANCE))
        }

        @Test
        fun `이벤트가 반복되면 점수가 누적된다`() {
            rankingScoreUpdater.incrementScore(1L, RankingEventType.VIEW, DATE)
            rankingScoreUpdater.incrementScore(1L, RankingEventType.VIEW, DATE)
            rankingScoreUpdater.incrementScore(1L, RankingEventType.LIKE, DATE)

            val score = redisTemplate.opsForZSet().score(KEY, "1")

            assertThat(score!!).isCloseTo(0.4, within(TOLERANCE))
        }

        @Test
        fun `주문 이벤트는 수량과 ORDER 가중치가 곱해져 반영된다`() {
            rankingScoreUpdater.incrementScore(1L, RankingEventType.ORDER, DATE, score = 3.0)

            val score = redisTemplate.opsForZSet().score(KEY, "1")

            assertThat(score!!).isCloseTo(2.1, within(TOLERANCE))
        }

        @Test
        fun `주문 1건이 좋아요 3건보다 높은 순위를 가진다`() {
            rankingScoreUpdater.incrementScore(ORDERED_PRODUCT, RankingEventType.ORDER, DATE)
            repeat(3) {
                rankingScoreUpdater.incrementScore(LIKED_PRODUCT, RankingEventType.LIKE, DATE)
            }

            val topRanked = redisTemplate.opsForZSet().reverseRange(KEY, 0, 1)

            assertThat(topRanked).containsExactly(ORDERED_PRODUCT.toString(), LIKED_PRODUCT.toString())
        }

        @Test
        fun `날짜가 다르면 서로 다른 키에 적재된다`() {
            rankingScoreUpdater.incrementScore(1L, RankingEventType.VIEW, DATE)
            rankingScoreUpdater.incrementScore(1L, RankingEventType.VIEW, DATE.plusDays(1))

            val todayScore = redisTemplate.opsForZSet().score(KEY, "1")
            val tomorrowScore = redisTemplate.opsForZSet().score("ranking:all:20260702", "1")

            assertThat(todayScore!!).isCloseTo(0.1, within(TOLERANCE))
            assertThat(tomorrowScore!!).isCloseTo(0.1, within(TOLERANCE))
        }

        @Test
        fun `랭킹 키에는 2일 TTL이 설정된다`() {
            rankingScoreUpdater.incrementScore(1L, RankingEventType.VIEW, DATE)

            val expireSeconds = redisTemplate.getExpire(KEY, TimeUnit.SECONDS)

            assertThat(expireSeconds).isBetween(1L, TWO_DAYS_SECONDS)
        }
    }

    @Nested
    inner class ColdStartCarryOver {

        @Test
        fun `전일 점수의 10퍼센트가 다음 날 키로 이월된다`() {
            val scheduler = RankingColdStartScheduler(rankingScoreUpdater, redisTemplate)
            val todayKey = rankingScoreUpdater.buildKey(DATE)
            val tomorrowKey = rankingScoreUpdater.buildKey(DATE.plusDays(1))
            redisTemplate.opsForZSet().add(todayKey, "1", 100.0)
            redisTemplate.opsForZSet().add(todayKey, "2", 50.0)

            scheduler.carryOverScores(DATE)

            assertThat(redisTemplate.opsForZSet().score(tomorrowKey, "1")).isCloseTo(10.0, within(TOLERANCE))
            assertThat(redisTemplate.opsForZSet().score(tomorrowKey, "2")).isCloseTo(5.0, within(TOLERANCE))
            assertThat(redisTemplate.getExpire(tomorrowKey, TimeUnit.SECONDS)).isBetween(1L, TWO_DAYS_SECONDS)
        }

        @Test
        fun `전일 점수가 없으면 이월하지 않는다`() {
            val scheduler = RankingColdStartScheduler(rankingScoreUpdater, redisTemplate)
            val tomorrowKey = rankingScoreUpdater.buildKey(DATE.plusDays(1))

            scheduler.carryOverScores(DATE)

            assertThat(redisTemplate.hasKey(tomorrowKey)).isFalse()
        }
    }

    companion object {
        private val DATE: LocalDate = LocalDate.of(2026, 7, 1)
        private const val KEY = "ranking:all:20260701"
        private const val TOLERANCE = 1e-9
        private const val TWO_DAYS_SECONDS = 2L * 24 * 60 * 60
        private const val ORDERED_PRODUCT = 1L
        private const val LIKED_PRODUCT = 2L

        private val redisContainer = GenericContainer(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379)
            .apply { start() }

        private val connectionFactory = LettuceConnectionFactory(
            redisContainer.host,
            redisContainer.getMappedPort(6379),
        ).apply {
            afterPropertiesSet()
            start()
        }

        private val redisTemplate = StringRedisTemplate(connectionFactory)

        @JvmStatic
        @AfterAll
        fun tearDownAll() {
            connectionFactory.destroy()
            redisContainer.stop()
        }
    }
}
