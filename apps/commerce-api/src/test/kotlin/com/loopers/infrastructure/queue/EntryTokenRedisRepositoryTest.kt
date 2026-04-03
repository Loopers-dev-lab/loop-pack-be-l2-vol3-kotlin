package com.loopers.infrastructure.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class EntryTokenRedisRepositoryTest @Autowired constructor(
    private val entryTokenRepository: EntryTokenRepository,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("issue & get")
    inner class IssueAndGet {

        @Test
        @DisplayName("issue 후 get으로 토큰을 조회할 수 있다")
        fun `issue 후 get으로 토큰을 조회할 수 있다`() {
            // given
            val userId = 1L
            val token = "test-token-abc"
            val ttlSeconds = 300L

            // when
            entryTokenRepository.issue(userId, token, ttlSeconds)
            val result = entryTokenRepository.get(userId)

            // then
            assertThat(result).isEqualTo(token)
        }
    }

    @Nested
    @DisplayName("consume")
    inner class Consume {

        @Test
        @DisplayName("consume 후 get이 null을 반환한다")
        fun `consume 후 get이 null을 반환한다`() {
            // given
            val userId = 1L
            val token = "test-token-abc"
            entryTokenRepository.issue(userId, token, 300L)

            // when
            entryTokenRepository.consume(userId)
            val result = entryTokenRepository.get(userId)

            // then
            assertThat(result).isNull()
        }
    }

    @Nested
    @DisplayName("TTL 만료")
    inner class TtlExpiry {

        @Test
        @DisplayName("TTL 만료 후 get이 null을 반환한다")
        fun `TTL 만료 후 get이 null을 반환한다`() {
            // given
            val userId = 1L
            val token = "test-token-ttl"
            entryTokenRepository.issue(userId, token, 1L)

            // when
            Thread.sleep(1_100)
            val result = entryTokenRepository.get(userId)

            // then
            assertThat(result).isNull()
        }
    }
}
