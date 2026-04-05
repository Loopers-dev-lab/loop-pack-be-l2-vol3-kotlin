package com.loopers.domain.queue

import com.loopers.testcontainers.RedisTestContainersConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import java.time.Duration

@SpringBootTest
@Import(RedisTestContainersConfig::class)
class TokenExpiryTest @Autowired constructor(
    private val queueRepository: QueueRepository,
) {

    companion object {
        private const val QUEUE_NAME = "test-queue"
        private const val USER_ID = 123L
        private const val TTL_SECONDS = 2L // 테스트용 짧은 TTL
    }

    @DisplayName("토큰 TTL - 만료 후 조회 불가")
    @Test
    fun `토큰이 TTL 만료 후 조회되지 않는다`() {
        // arrange: 토큰 발급
        val token = "test-token-abc123"
        queueRepository.issueToken(QUEUE_NAME, USER_ID, token, TTL_SECONDS)

        // act & assert 1단계: 발급 직후 토큰 존재
        val tokenBeforeExpiry = queueRepository.getToken(QUEUE_NAME, USER_ID)
        assertThat(tokenBeforeExpiry).isEqualTo(token)

        // act & assert 2단계: TTL 대기 후 토큰 만료
        Thread.sleep(Duration.ofSeconds(TTL_SECONDS + 1).toMillis())
        val tokenAfterExpiry = queueRepository.getToken(QUEUE_NAME, USER_ID)
        assertThat(tokenAfterExpiry).isNull()
    }

    @DisplayName("토큰 TTL - 정상 TTL 유지")
    @Test
    fun `토큰이 TTL 이내에는 조회된다`() {
        // arrange: 토큰 발급 (3초 TTL)
        val token = "test-token-def456"
        queueRepository.issueToken(QUEUE_NAME, USER_ID + 1, token, 3L)

        // act & assert: 1초 대기 후에도 여전히 존재
        Thread.sleep(1000)
        val retrievedToken = queueRepository.getToken(QUEUE_NAME, USER_ID + 1)
        assertThat(retrievedToken).isEqualTo(token)
    }
}
