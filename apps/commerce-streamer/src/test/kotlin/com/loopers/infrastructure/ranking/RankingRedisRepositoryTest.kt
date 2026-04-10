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
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class RankingRedisRepositoryTest {

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

    @DisplayName("랭킹 점수를 증가시킬 때,")
    @Nested
    inner class IncrementScore {

        @DisplayName("올바른 ZSET 키에 ZINCRBY로 점수를 누적한다.")
        @Test
        fun incrementsScore_withCorrectKey() {
            // arrange
            val productId = 101L
            val score = 0.1
            val date = LocalDate.of(2026, 4, 8)
            val expectedKey = "ranking:all:20260408"

            // act
            rankingRedisRepository.incrementScore(productId, score, date)

            // assert
            verify(zSetOperations).incrementScore(expectedKey, "101", score)
        }

        @DisplayName("키에 TTL 2일을 설정한다.")
        @Test
        fun setsTtl_whenIncrementingScore() {
            // arrange
            val productId = 101L
            val score = 0.2
            val date = LocalDate.of(2026, 4, 8)
            val expectedKey = "ranking:all:20260408"

            // act
            rankingRedisRepository.incrementScore(productId, score, date)

            // assert
            verify(masterRedisTemplate).expire(expectedKey, Duration.ofDays(2))
        }
    }

    @DisplayName("ZSET 키를 생성할 때,")
    @Nested
    inner class BuildKey {

        @DisplayName("날짜가 다르면, 다른 키가 생성된다.")
        @Test
        fun buildsDifferentKey_whenDifferentDate() {
            // arrange
            val date1 = LocalDate.of(2026, 4, 7)
            val date2 = LocalDate.of(2026, 4, 8)

            // act
            rankingRedisRepository.incrementScore(1L, 0.1, date1)
            rankingRedisRepository.incrementScore(1L, 0.1, date2)

            // assert
            verify(zSetOperations).incrementScore("ranking:all:20260407", "1", 0.1)
            verify(zSetOperations).incrementScore("ranking:all:20260408", "1", 0.1)
        }
    }
}
