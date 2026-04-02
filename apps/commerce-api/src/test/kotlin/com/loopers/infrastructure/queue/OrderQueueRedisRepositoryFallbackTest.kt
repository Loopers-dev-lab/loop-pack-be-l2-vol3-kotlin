package com.loopers.infrastructure.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ZSetOperations

@ExtendWith(MockitoExtension::class)
class OrderQueueRedisRepositoryFallbackTest {

    @Mock
    private lateinit var masterRedisTemplate: RedisTemplate<String, String>

    @Mock
    private lateinit var readRedisTemplate: RedisTemplate<String, String>

    private lateinit var repository: OrderQueueRedisRepository

    @BeforeEach
    fun setUp() {
        whenever(masterRedisTemplate.opsForZSet()).thenReturn(mock<ZSetOperations<String, String>>())
        whenever(readRedisTemplate.opsForZSet()).thenReturn(mock<ZSetOperations<String, String>>())
        repository = OrderQueueRedisRepository(masterRedisTemplate, readRedisTemplate)
    }

    @Nested
    @DisplayName("Redis 장애 시 fallback 동작")
    inner class Fallback {

        private val redisException = RuntimeException("Redis connection failed")

        @Test
        @DisplayName("enqueueFallback — true를 반환한다 (bypass)")
        fun enqueueFallback_returnsTrue() {
            // act
            val result = repository.enqueueFallback(1L, 1000.0, redisException)

            // assert
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("getPositionFallback — null을 반환한다")
        fun getPositionFallback_returnsNull() {
            // act
            val result = repository.getPositionFallback(1L, redisException)

            // assert
            assertThat(result).isNull()
        }

        @Test
        @DisplayName("getTotalSizeFallback — 0을 반환한다")
        fun getTotalSizeFallback_returnsZero() {
            // act
            val result = repository.getTotalSizeFallback(redisException)

            // assert
            assertThat(result).isEqualTo(0L)
        }

        @Test
        @DisplayName("popFrontFallback — 빈 목록을 반환한다")
        fun popFrontFallback_returnsEmptyList() {
            // act
            val result = repository.popFrontFallback(10L, redisException)

            // assert
            assertThat(result).isEmpty()
        }
    }
}
