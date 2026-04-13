package com.loopers.infrastructure.ranking

import com.loopers.config.redis.RedisRankingConstants
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.RedisTemplate
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RedisRankingScoreRepositoryTest @Autowired constructor(
    private val redisRankingScoreRepository: RedisRankingScoreRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisCleanUp: RedisCleanUp,
    private val clock: Clock,
) {

    private val today: LocalDate get() = LocalDate.now(clock)

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("incrementScore")
    inner class IncrementScore {

        @Test
        @DisplayName("ZINCRBY로 점수 누적 시 정확한 값이 반영된다")
        fun `점수가 정확히 누적된다`() {
            // Arrange
            val productId = 1L

            // Act
            redisRankingScoreRepository.incrementScore(productId, 0.1, "evt-1", today)
            redisRankingScoreRepository.incrementScore(productId, 0.2, "evt-2", today)

            // Assert
            val key = findRankingKey()
            val score = redisTemplate.opsForZSet().score(key, productId.toString())
            assertThat(score).isCloseTo(0.3, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        @DisplayName("서로 다른 상품의 점수가 독립적으로 관리된다")
        fun `서로 다른 상품의 점수가 독립적으로 관리된다`() {
            // Arrange & Act
            redisRankingScoreRepository.incrementScore(1L, 0.5, "evt-1", today)
            redisRankingScoreRepository.incrementScore(2L, 1.0, "evt-2", today)

            // Assert
            val key = findRankingKey()
            assertThat(redisTemplate.opsForZSet().score(key, "1")).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.001))
            assertThat(redisTemplate.opsForZSet().score(key, "2")).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        @DisplayName("음수 점수로 감소시킬 수 있다")
        fun `음수 점수로 감소시킬 수 있다`() {
            // Arrange
            redisRankingScoreRepository.incrementScore(1L, 0.2, "evt-1", today)

            // Act
            redisRankingScoreRepository.incrementScore(1L, -0.2, "evt-2", today)

            // Assert
            val key = findRankingKey()
            val score = redisTemplate.opsForZSet().score(key, "1")
            assertThat(score).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.001))
        }
    }

    @Nested
    @DisplayName("TTL 설정")
    inner class TtlSetting {

        @Test
        @DisplayName("키 최초 생성 시 TTL 2일이 설정된다")
        fun `키 최초 생성 시 TTL이 설정된다`() {
            // Act
            redisRankingScoreRepository.incrementScore(1L, 0.1, "evt-1", today)

            // Assert
            val key = findRankingKey()
            val ttl = redisTemplate.getExpire(key)
            assertThat(ttl).isGreaterThan(0L)
            assertThat(ttl).isLessThanOrEqualTo(RedisRankingConstants.RANKING_TTL_SECONDS)
        }

        @Test
        @DisplayName("후속 incrementScore 호출은 TTL을 연장하지 않는다")
        fun `후속 호출은 TTL을 연장하지 않는다`() {
            // Arrange — 최초 호출로 키 생성 + TTL 설정
            redisRankingScoreRepository.incrementScore(1L, 0.1, "evt-1", today)
            val key = findRankingKey()

            // TTL을 100초로 수동 축소하여 비연장 검증 가능하게 설정
            redisTemplate.expire(key, 100L, TimeUnit.SECONDS)
            val ttlBefore = redisTemplate.getExpire(key)

            // Act — 후속 호출
            redisRankingScoreRepository.incrementScore(1L, 0.2, "evt-2", today)

            // Assert — TTL이 원래 값(172,800초)으로 리셋되지 않음
            val ttlAfter = redisTemplate.getExpire(key)
            assertThat(ttlAfter).isLessThanOrEqualTo(ttlBefore)
            assertThat(ttlAfter).isLessThanOrEqualTo(100L)
        }

        @Test
        @DisplayName("키가 수동 삭제된 후 score 조회 시 null을 반환한다")
        fun `키 삭제 후 score 조회는 null을 반환한다`() {
            // Arrange — 점수 등록 후 키 직접 삭제
            redisRankingScoreRepository.incrementScore(1L, 0.5, "evt-1", today)
            val key = findRankingKey()
            redisTemplate.delete(key)

            // Assert — 키 없음 + score null 반환
            assertThat(redisTemplate.hasKey(key) ?: false).isFalse()
            assertThat(redisTemplate.opsForZSet().score(key, "1") as Any?).isNull()
        }

        @Test
        @DisplayName("incrementScore 호출 시 processed-event set에도 TTL이 설정된다")
        fun `processed-event set에 TTL이 설정된다`() {
            // Arrange & Act
            redisRankingScoreRepository.incrementScore(1L, 0.5, "evt-1", today)

            // Assert — processed-event set에 TTL이 설정되어 자동 만료됨
            val processedKey = "ranking:processed:${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"
            val ttl = redisTemplate.getExpire(processedKey)
            assertThat(ttl).isGreaterThan(0L)
            assertThat(ttl).isLessThanOrEqualTo(RedisRankingConstants.RANKING_TTL_SECONDS)
        }

        @Test
        @DisplayName("ranking key가 TTL 만료 후 실제로 삭제된다")
        fun `ranking key가 TTL 만료 후 실제로 삭제된다`() {
            // Arrange — key 생성 후 TTL을 1초로 축소
            redisRankingScoreRepository.incrementScore(1L, 0.5, "evt-1", today)
            val key = findRankingKey()
            redisTemplate.expire(key, 1L, TimeUnit.SECONDS)

            // Assert — 폴링으로 실제 만료 확인
            assertThat(pollUntilKeyAbsent(key)).isTrue()
        }

        @Test
        @DisplayName("processed-event key가 TTL 만료 후 실제로 삭제된다")
        fun `processed-event key가 TTL 만료 후 실제로 삭제된다`() {
            // Arrange — key 생성 후 TTL을 1초로 축소
            redisRankingScoreRepository.incrementScore(1L, 0.5, "evt-1", today)
            val processedKey = "ranking:processed:${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"
            redisTemplate.expire(processedKey, 1L, TimeUnit.SECONDS)

            // Assert — 폴링으로 실제 만료 확인
            assertThat(pollUntilKeyAbsent(processedKey)).isTrue()
        }
    }

    @Nested
    @DisplayName("멱등성")
    inner class Idempotency {

        @Test
        @DisplayName("동일 eventId로 2회 호출 시 1회만 반영된다")
        fun `동일 eventId로 2회 호출 시 1회만 반영된다`() {
            // Arrange & Act
            redisRankingScoreRepository.incrementScore(1L, 0.5, "evt-same", today)
            redisRankingScoreRepository.incrementScore(1L, 0.5, "evt-same", today)

            // Assert — 1회만 반영되어 0.5
            val key = findRankingKey()
            val score = redisTemplate.opsForZSet().score(key, "1")
            assertThat(score).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        @DisplayName("서로 다른 eventId로 호출하면 각각 반영된다")
        fun `서로 다른 eventId로 호출하면 각각 반영된다`() {
            // Arrange & Act
            redisRankingScoreRepository.incrementScore(1L, 0.5, "evt-1", today)
            redisRankingScoreRepository.incrementScore(1L, 0.5, "evt-2", today)

            // Assert — 2회 반영되어 1.0
            val key = findRankingKey()
            val score = redisTemplate.opsForZSet().score(key, "1")
            assertThat(score).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001))
        }
    }

    @Nested
    @DisplayName("rankingDate 기반 키 생성")
    inner class RankingDateKey {

        @Test
        @DisplayName("전달된 rankingDate로 랭킹 키가 생성된다")
        fun `전달된 rankingDate로 랭킹 키가 생성된다`() {
            // Arrange — 오늘과 다른 날짜로 키 생성 검증
            val customDate = LocalDate.of(2025, 1, 15)
            val expectedKey = "${RedisRankingConstants.RANKING_KEY_PREFIX}${customDate.format(DateTimeFormatter.BASIC_ISO_DATE)}"

            // Act
            redisRankingScoreRepository.incrementScore(1L, 0.1, "evt-1", customDate)

            // Assert — 오늘 날짜가 아닌 전달된 날짜로 키가 생성됨
            assertThat(redisTemplate.hasKey(expectedKey)).isTrue()
        }

        @Test
        @DisplayName("서로 다른 날짜의 점수가 독립적으로 관리된다")
        fun `서로 다른 날짜의 점수가 독립적으로 관리된다`() {
            // Arrange
            val yesterday = today.minusDays(1)
            val todayKey = "${RedisRankingConstants.RANKING_KEY_PREFIX}${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"
            val yesterdayKey = "${RedisRankingConstants.RANKING_KEY_PREFIX}${yesterday.format(DateTimeFormatter.BASIC_ISO_DATE)}"

            // Act
            redisRankingScoreRepository.incrementScore(1L, 0.5, "evt-today", today)
            redisRankingScoreRepository.incrementScore(1L, 1.0, "evt-yesterday", yesterday)

            // Assert — 날짜별 키가 분리되어 서로 간섭 없음
            assertThat(redisTemplate.opsForZSet().score(todayKey, "1")).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.001))
            assertThat(redisTemplate.opsForZSet().score(yesterdayKey, "1")).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001))
        }
    }

    private fun pollUntilKeyAbsent(key: String, timeoutMs: Long = 5000L, intervalMs: Long = 100L): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (redisTemplate.hasKey(key) != true) return true
            Thread.sleep(intervalMs)
        }
        return false
    }

    private fun findRankingKey(): String {
        val keys = redisTemplate.keys("${RedisRankingConstants.RANKING_KEY_PREFIX}*")
        return keys.first()
    }
}
