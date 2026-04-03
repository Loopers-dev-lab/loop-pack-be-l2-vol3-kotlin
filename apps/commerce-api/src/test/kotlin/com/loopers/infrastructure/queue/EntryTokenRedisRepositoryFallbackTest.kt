package com.loopers.infrastructure.queue

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ValueOperations

@ExtendWith(MockitoExtension::class)
class EntryTokenRedisRepositoryFallbackTest {

    @Mock
    private lateinit var masterRedisTemplate: RedisTemplate<String, String>

    private lateinit var repository: EntryTokenRedisRepository

    @BeforeEach
    fun setUp() {
        whenever(masterRedisTemplate.opsForValue()).thenReturn(mock<ValueOperations<String, String>>())
        repository = EntryTokenRedisRepository(masterRedisTemplate)
    }

    @Nested
    @DisplayName("Redis 장애 시 fallback 동작")
    inner class Fallback {

        private val redisException = RuntimeException("Redis connection failed")

        @Test
        @DisplayName("getFallback — null을 반환한다")
        fun getFallback_returnsNull() {
            // act
            val result = repository.getFallback(1L, redisException)

            // assert
            assertThat(result).isNull()
        }

        @Test
        @DisplayName("issueFallback — 예외 없이 무시한다")
        fun issueFallback_doesNotThrow() {
            // act & assert
            assertDoesNotThrow {
                repository.issueFallback(1L, "token", 300L, redisException)
            }
        }

        @Test
        @DisplayName("consumeIfMatchesFallback — false를 반환한다")
        fun consumeIfMatchesFallback_returnsFalse() {
            // act
            val result = repository.consumeIfMatchesFallback(1L, "any-token", redisException)

            // assert
            assertThat(result).isFalse()
        }
    }
}
