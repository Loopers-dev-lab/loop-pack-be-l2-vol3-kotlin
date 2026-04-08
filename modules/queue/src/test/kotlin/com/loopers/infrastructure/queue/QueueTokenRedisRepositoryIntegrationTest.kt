package com.loopers.infrastructure.queue

import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(RedisTestContainersConfig::class)
class QueueTokenRedisRepositoryIntegrationTest @Autowired constructor(
    private val queueTokenRedisRepository: QueueTokenRedisRepository,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun cleanUp() {
        redisCleanUp.truncateAll()
    }

    @DisplayName("issueToken")
    @Nested
    inner class IssueToken {
        @DisplayName("토큰을 발급하면, UUID 형식의 토큰을 반환한다.")
        @Test
        fun returnsUuidToken() {
            // act
            val token = queueTokenRedisRepository.issueToken(1L, 300)

            // assert
            assertAll(
                { assertThat(token).isNotBlank() },
                { assertThat(token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") },
            )
        }
    }

    @DisplayName("getToken")
    @Nested
    inner class GetToken {
        @DisplayName("발급된 토큰을 조회할 수 있다.")
        @Test
        fun returnsToken_whenExists() {
            // arrange
            val issued = queueTokenRedisRepository.issueToken(1L, 300)

            // act
            val token = queueTokenRedisRepository.getToken(1L)

            // assert
            assertThat(token).isEqualTo(issued)
        }

        @DisplayName("토큰이 없으면 null을 반환한다.")
        @Test
        fun returnsNull_whenNotExists() {
            // act
            val token = queueTokenRedisRepository.getToken(999L)

            // assert
            assertThat(token).isNull()
        }
    }

    @DisplayName("deleteToken")
    @Nested
    inner class DeleteToken {
        @DisplayName("토큰을 삭제하면 조회되지 않는다.")
        @Test
        fun deletesToken() {
            // arrange
            queueTokenRedisRepository.issueToken(1L, 300)

            // act
            val deleted = queueTokenRedisRepository.deleteToken(1L)

            // assert
            assertAll(
                { assertThat(deleted).isTrue() },
                { assertThat(queueTokenRedisRepository.getToken(1L)).isNull() },
            )
        }
    }

    @DisplayName("hasToken")
    @Nested
    inner class HasToken {
        @DisplayName("토큰이 존재하면 true를 반환한다.")
        @Test
        fun returnsTrue_whenTokenExists() {
            // arrange
            queueTokenRedisRepository.issueToken(1L, 300)

            // act & assert
            assertThat(queueTokenRedisRepository.hasToken(1L)).isTrue()
        }

        @DisplayName("토큰이 없으면 false를 반환한다.")
        @Test
        fun returnsFalse_whenTokenNotExists() {
            // act & assert
            assertThat(queueTokenRedisRepository.hasToken(999L)).isFalse()
        }
    }

    @DisplayName("countActiveTokens")
    @Nested
    inner class CountActiveTokens {
        @DisplayName("발급된 토큰 수를 정확히 반환한다.")
        @Test
        fun returnsExactCount() {
            // arrange
            queueTokenRedisRepository.issueToken(1L, 300)
            queueTokenRedisRepository.issueToken(2L, 300)
            queueTokenRedisRepository.issueToken(3L, 300)

            // act
            val count = queueTokenRedisRepository.countActiveTokens()

            // assert
            assertThat(count).isEqualTo(3L)
        }

        @DisplayName("토큰이 없으면 0을 반환한다.")
        @Test
        fun returnsZero_whenNoTokens() {
            // act & assert
            assertThat(queueTokenRedisRepository.countActiveTokens()).isEqualTo(0L)
        }

        @DisplayName("삭제된 토큰은 카운트에 포함되지 않는다.")
        @Test
        fun excludesDeletedTokens() {
            // arrange
            queueTokenRedisRepository.issueToken(1L, 300)
            queueTokenRedisRepository.issueToken(2L, 300)
            queueTokenRedisRepository.deleteToken(1L)

            // act
            val count = queueTokenRedisRepository.countActiveTokens()

            // assert
            assertThat(count).isEqualTo(1L)
        }
    }

    @DisplayName("카운터 연산")
    @Nested
    inner class ActiveTokenCounter {
        @DisplayName("incrementActiveTokenCount로 카운터를 증가시킨다.")
        @Test
        fun incrementsCounter() {
            // act
            val result = queueTokenRedisRepository.incrementActiveTokenCount(5)

            // assert
            assertAll(
                { assertThat(result).isEqualTo(5L) },
                { assertThat(queueTokenRedisRepository.getActiveTokenCount()).isEqualTo(5L) },
            )
        }

        @DisplayName("decrementActiveTokenCount로 카운터를 감소시킨다.")
        @Test
        fun decrementsCounter() {
            // arrange
            queueTokenRedisRepository.incrementActiveTokenCount(10)

            // act
            val result = queueTokenRedisRepository.decrementActiveTokenCount()

            // assert
            assertThat(result).isEqualTo(9L)
        }

        @DisplayName("카운터가 0 미만으로 내려가면 0으로 보정된다.")
        @Test
        fun floorsAtZero() {
            // act
            val result = queueTokenRedisRepository.decrementActiveTokenCount()

            // assert
            assertThat(result).isEqualTo(0L)
        }

        @DisplayName("setActiveTokenCount로 카운터를 직접 설정한다.")
        @Test
        fun setsCounter() {
            // act
            queueTokenRedisRepository.setActiveTokenCount(42)

            // assert
            assertThat(queueTokenRedisRepository.getActiveTokenCount()).isEqualTo(42L)
        }

        @DisplayName("countActiveTokens(SCAN)와 카운터 키가 분리된다.")
        @Test
        fun scanExcludesCounterKey() {
            // arrange
            queueTokenRedisRepository.issueToken(1L, 300)
            queueTokenRedisRepository.issueToken(2L, 300)
            queueTokenRedisRepository.setActiveTokenCount(99)

            // act
            val scanCount = queueTokenRedisRepository.countActiveTokens()

            // assert - SCAN은 토큰 키만 세고 카운터 키는 제외
            assertThat(scanCount).isEqualTo(2L)
        }
    }

    @DisplayName("토큰 TTL 만료")
    @Nested
    inner class TokenExpiry {
        @DisplayName("TTL이 만료되면 토큰이 자동 삭제된다.")
        @Test
        fun tokenExpiresAfterTtl() {
            // arrange
            queueTokenRedisRepository.issueToken(1L, 1)

            // act
            Thread.sleep(2000)

            // assert
            assertThat(queueTokenRedisRepository.hasToken(1L)).isFalse()
        }
    }
}
