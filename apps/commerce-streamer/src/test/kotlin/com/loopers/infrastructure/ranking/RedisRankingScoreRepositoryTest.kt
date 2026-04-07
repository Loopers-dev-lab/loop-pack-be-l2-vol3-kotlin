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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RedisRankingScoreRepositoryTest @Autowired constructor(
    private val redisRankingScoreRepository: RedisRankingScoreRepository,
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisCleanUp: RedisCleanUp,
) {

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
            redisRankingScoreRepository.incrementScore(productId, 0.1)
            redisRankingScoreRepository.incrementScore(productId, 0.2)

            // Assert
            val key = findRankingKey()
            val score = redisTemplate.opsForZSet().score(key, productId.toString())
            assertThat(score).isCloseTo(0.3, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        @DisplayName("서로 다른 상품의 점수가 독립적으로 관리된다")
        fun `서로 다른 상품의 점수가 독립적으로 관리된다`() {
            // Arrange & Act
            redisRankingScoreRepository.incrementScore(1L, 0.5)
            redisRankingScoreRepository.incrementScore(2L, 1.0)

            // Assert
            val key = findRankingKey()
            assertThat(redisTemplate.opsForZSet().score(key, "1")).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.001))
            assertThat(redisTemplate.opsForZSet().score(key, "2")).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001))
        }

        @Test
        @DisplayName("음수 점수로 감소시킬 수 있다")
        fun `음수 점수로 감소시킬 수 있다`() {
            // Arrange
            redisRankingScoreRepository.incrementScore(1L, 0.2)

            // Act
            redisRankingScoreRepository.incrementScore(1L, -0.2)

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
            redisRankingScoreRepository.incrementScore(1L, 0.1)

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
            redisRankingScoreRepository.incrementScore(1L, 0.1)
            val key = findRankingKey()

            // TTL을 100초로 수동 축소하여 비연장 검증 가능하게 설정
            redisTemplate.expire(key, 100L, java.util.concurrent.TimeUnit.SECONDS)
            val ttlBefore = redisTemplate.getExpire(key)

            // Act — 후속 호출
            redisRankingScoreRepository.incrementScore(1L, 0.2)

            // Assert — TTL이 원래 값(172,800초)으로 리셋되지 않음
            val ttlAfter = redisTemplate.getExpire(key)
            assertThat(ttlAfter).isLessThanOrEqualTo(ttlBefore)
            assertThat(ttlAfter).isLessThanOrEqualTo(100L)
        }
    }

    private fun findRankingKey(): String {
        val keys = redisTemplate.keys("${RedisRankingConstants.RANKING_KEY_PREFIX}*")
        return keys.first()
    }
}
