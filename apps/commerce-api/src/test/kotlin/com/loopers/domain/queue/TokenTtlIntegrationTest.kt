package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.testcontainers.RedisTestContainersConfig
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

/**
 * 토큰 TTL 만료 통합 테스트.
 * FakeTokenRepository는 TTL을 실제로 만료시키지 않으므로,
 * 실제 Redis가 필요한 이 케이스는 @SpringBootTest로 검증한다.
 * Testcontainers로 Redis를 자동 실행하므로 로컬 Redis 없이도 동작한다.
 */
@SpringBootTest
@Import(RedisTestContainersConfig::class)
class TokenTtlIntegrationTest @Autowired constructor(
    private val tokenRepository: TokenRepository,
    private val queueService: QueueService,
    private val redisCleanUp: RedisCleanUp,
) {

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Test
    @DisplayName("TTL이 만료된 토큰으로 검증하면 INVALID_TOKEN 예외가 발생한다")
    fun expiredTokenThrowsInvalidToken() {
        // arrange — TTL 1초짜리 토큰 저장
        val userId = 1L
        val token = "ttl-test-token"
        tokenRepository.save(userId, token, ttlSeconds = 1L)

        // TTL 만료 대기 (2초)
        Thread.sleep(2_000)

        // act & assert — Redis 키 만료 → get() == null → INVALID_TOKEN
        val exception = assertThrows<CoreException> {
            queueService.validateAndConsumeToken(userId, token)
        }
        assertThat(exception.errorType).isEqualTo(ErrorType.INVALID_TOKEN)
    }

    @Test
    @DisplayName("TTL 내에 사용한 토큰은 검증에 성공한다")
    fun tokenWithinTtlIsValid() {
        // arrange
        val userId = 1L
        val token = "valid-ttl-token"
        tokenRepository.save(userId, token, ttlSeconds = 10L)

        // act & assert — 만료 전이므로 예외 없이 통과
        queueService.validateAndConsumeToken(userId, token)

        // 소비 후 토큰 삭제 확인
        assertThat(tokenRepository.get(userId)).isNull()
    }
}
