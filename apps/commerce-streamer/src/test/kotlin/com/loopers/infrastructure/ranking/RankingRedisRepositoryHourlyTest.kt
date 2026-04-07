package com.loopers.infrastructure.ranking

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations
import java.time.Duration
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class RankingRedisRepositoryHourlyTest {

    @Mock
    private lateinit var masterRedisTemplate: RedisTemplate<String, String>

    @Mock
    private lateinit var zSetOperations: ZSetOperations<String, String>

    private lateinit var rankingRedisRepository: RankingRedisRepository

    @BeforeEach
    fun setUp() {
        whenever(masterRedisTemplate.opsForZSet()).thenReturn(zSetOperations)
        rankingRedisRepository = RankingRedisRepository(masterRedisTemplate)
    }

    @DisplayName("시간 단위 랭킹 점수를 증가시킬 때,")
    @Nested
    inner class IncrementHourlyScore {

        @DisplayName("올바른 시간 단위 ZSET 키에 점수를 누적한다.")
        @Test
        fun incrementsScore_withHourlyKey() {
            // arrange
            val productId = 101L
            val score = 0.1
            val dateTime = LocalDateTime.of(2026, 4, 8, 14, 30)
            val expectedKey = "ranking:hourly:2026040814"

            // act
            rankingRedisRepository.incrementHourlyScore(productId, score, dateTime)

            // assert
            verify(zSetOperations).incrementScore(expectedKey, "101", score)
        }

        @DisplayName("키에 TTL 3시간을 설정한다.")
        @Test
        fun setsTtl_whenIncrementingHourlyScore() {
            // arrange
            val productId = 101L
            val score = 0.2
            val dateTime = LocalDateTime.of(2026, 4, 8, 14, 30)
            val expectedKey = "ranking:hourly:2026040814"

            // act
            rankingRedisRepository.incrementHourlyScore(productId, score, dateTime)

            // assert
            verify(masterRedisTemplate).expire(expectedKey, Duration.ofHours(3))
        }
    }
}
