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
    @DisplayName("consumeIfMatches")
    inner class ConsumeIfMatches {

        @Test
        @DisplayName("토큰이 일치하면 삭제하고 true를 반환한다")
        fun `토큰이 일치하면 삭제하고 true를 반환한다`() {
            // given
            val userId = 1L
            val token = "test-token-abc"
            entryTokenRepository.issue(userId, token, 300L)

            // when
            val result = entryTokenRepository.consumeIfMatches(userId, token)

            // then
            assertThat(result).isTrue()
            assertThat(entryTokenRepository.get(userId)).isNull()
        }

        @Test
        @DisplayName("토큰이 불일치하면 삭제하지 않고 false를 반환한다")
        fun `토큰이 불일치하면 삭제하지 않고 false를 반환한다`() {
            // given
            val userId = 1L
            val token = "test-token-abc"
            entryTokenRepository.issue(userId, token, 300L)

            // when
            val result = entryTokenRepository.consumeIfMatches(userId, "wrong-token")

            // then
            assertThat(result).isFalse()
            assertThat(entryTokenRepository.get(userId)).isEqualTo(token)
        }

        @Test
        @DisplayName("토큰이 없으면 false를 반환한다")
        fun `토큰이 없으면 false를 반환한다`() {
            // when
            val result = entryTokenRepository.consumeIfMatches(999L, "any-token")

            // then
            assertThat(result).isFalse()
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
