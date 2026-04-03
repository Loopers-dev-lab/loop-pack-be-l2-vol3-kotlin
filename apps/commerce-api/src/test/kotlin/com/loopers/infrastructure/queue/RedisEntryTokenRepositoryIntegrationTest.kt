package com.loopers.infrastructure.queue

import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("RedisEntryTokenRepository 통합 테스트")
@SpringBootTest
class RedisEntryTokenRepositoryIntegrationTest
@Autowired
constructor(
    private val entryTokenRepository: EntryTokenRepository,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("토큰을 발급한다")
    inner class Issue {
        @Test
        @DisplayName("토큰 발급 시 EntryToken을 반환한다")
        fun issue_success() {
            val token = entryTokenRepository.issue(1L)

            assertAll(
                { assertThat(token.token).isNotBlank() },
                { assertThat(token.userId).isEqualTo(1L) },
            )
            assertThat(entryTokenRepository.exists(1L)).isTrue()
        }

        @Test
        @DisplayName("발급된 토큰의 TTL이 300초로 설정된다")
        fun issue_ttlIs300Seconds() {
            entryTokenRepository.issue(1L)
            val found = entryTokenRepository.findByUserId(1L)
            assertThat(found).isNotNull
            assertThat(found!!.remainingSeconds).isBetween(299, 300)
        }
    }

    @Nested
    @DisplayName("토큰을 검증하고 소비한다 (Lua 원자적)")
    inner class ValidateAndConsume {
        @Test
        @DisplayName("올바른 토큰이면 true를 반환하고 키를 삭제한다")
        fun validateAndConsume_validToken() {
            val token = entryTokenRepository.issue(1L)

            val result = entryTokenRepository.validateAndConsume(1L, token.token)

            assertAll(
                { assertThat(result).isTrue() },
                { assertThat(entryTokenRepository.exists(1L)).isFalse() },
            )
        }

        @Test
        @DisplayName("잘못된 토큰이면 false를 반환하고 키를 유지한다")
        fun validateAndConsume_invalidToken() {
            entryTokenRepository.issue(1L)

            val result = entryTokenRepository.validateAndConsume(1L, "wrong-token")

            assertAll(
                { assertThat(result).isFalse() },
                { assertThat(entryTokenRepository.exists(1L)).isTrue() },
            )
        }

        @Test
        @DisplayName("존재하지 않는 토큰이면 false를 반환한다")
        fun validateAndConsume_expiredToken() {
            val result = entryTokenRepository.validateAndConsume(999L, "any-token")

            assertThat(result).isFalse()
        }

        @Test
        @DisplayName("토큰 재발급 후 이전 토큰은 무효화된다")
        fun validateAndConsume_afterReissue() {
            val first = entryTokenRepository.issue(1L)
            val second = entryTokenRepository.issue(1L)
            assertAll(
                { assertThat(entryTokenRepository.validateAndConsume(1L, first.token)).isFalse() },
                { assertThat(entryTokenRepository.validateAndConsume(1L, second.token)).isTrue() },
            )
        }

        @Test
        @DisplayName("동일 토큰 2스레드 동시 호출 시 정확히 1회만 true")
        fun validateAndConsume_concurrent() {
            val token = entryTokenRepository.issue(1L)
            val successCount = AtomicInteger(0)
            val latch = CountDownLatch(2)
            val executor = Executors.newFixedThreadPool(2)

            repeat(2) {
                executor.submit {
                    try {
                        if (entryTokenRepository.validateAndConsume(1L, token.token)) {
                            successCount.incrementAndGet()
                        }
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await()
            executor.shutdown()
            assertThat(successCount.get()).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("토큰 존재 여부를 확인한다")
    inner class Exists {
        @Test
        @DisplayName("토큰이 있으면 true")
        fun exists_hasToken() {
            entryTokenRepository.issue(1L)

            assertThat(entryTokenRepository.exists(1L)).isTrue()
        }

        @Test
        @DisplayName("토큰이 없으면 false")
        fun exists_noToken() {
            assertThat(entryTokenRepository.exists(999L)).isFalse()
        }
    }

    @Nested
    @DisplayName("토큰을 조회한다")
    inner class FindByUserId {
        @Test
        @DisplayName("토큰이 있으면 EntryToken을 반환한다")
        fun findByUserId_exists() {
            val issued = entryTokenRepository.issue(1L)

            val found = entryTokenRepository.findByUserId(1L)

            assertThat(found).isNotNull
            assertAll(
                { assertThat(found!!.token).isEqualTo(issued.token) },
                { assertThat(found!!.userId).isEqualTo(1L) },
                { assertThat(found!!.remainingSeconds).isLessThanOrEqualTo(300) },
            )
        }

        @Test
        @DisplayName("토큰이 없으면 null을 반환한다")
        fun findByUserId_notExists() {
            assertThat(entryTokenRepository.findByUserId(999L)).isNull()
        }
    }
}
