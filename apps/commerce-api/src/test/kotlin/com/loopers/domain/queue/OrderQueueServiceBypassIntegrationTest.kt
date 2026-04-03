package com.loopers.domain.queue

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

@SpringBootTest
class OrderQueueServiceBypassIntegrationTest @Autowired constructor(
    private val orderQueueService: OrderQueueService,
    private val entryTokenRepository: EntryTokenRepository,
    private val redisCleanUp: RedisCleanUp,
) {

    @MockitoBean
    private lateinit var queueHealthChecker: QueueHealthChecker

    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    @Nested
    @DisplayName("bypass 모드가 아닐 때 (CircuitBreaker CLOSED),")
    inner class WhenNotBypassed {

        @Test
        @DisplayName("유효한 토큰이면 원자적으로 소비하고 정상 종료한다")
        fun consumesToken_whenValidToken() {
            // arrange
            val userId = 1L
            val token = UUID.randomUUID().toString()
            whenever(queueHealthChecker.isBypassed()).thenReturn(false)
            entryTokenRepository.issue(userId, token, 300L)

            // act
            orderQueueService.validateAndConsumeToken(userId, token)

            // assert — 토큰이 원자적으로 소비되어 다시 조회하면 null
            assertThat(entryTokenRepository.get(userId)).isNull()
        }

        @Test
        @DisplayName("잘못된 토큰이면 FORBIDDEN 예외가 발생한다")
        fun throwsForbidden_whenTokenMismatch() {
            // arrange
            val userId = 2L
            val correctToken = UUID.randomUUID().toString()
            whenever(queueHealthChecker.isBypassed()).thenReturn(false)
            entryTokenRepository.issue(userId, correctToken, 300L)

            // act
            val exception = assertThrows<CoreException> {
                orderQueueService.validateAndConsumeToken(userId, "wrong-token")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @Test
        @DisplayName("토큰이 없으면 FORBIDDEN 예외가 발생한다")
        fun throwsForbidden_whenNoToken() {
            // arrange
            val userId = 3L
            whenever(queueHealthChecker.isBypassed()).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                orderQueueService.validateAndConsumeToken(userId, "any-token")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }

    @Nested
    @DisplayName("bypass 모드일 때 (CircuitBreaker OPEN),")
    inner class WhenBypassed {

        @Test
        @DisplayName("토큰 없이도 검증을 스킵하고 정상 종료한다")
        fun skipsValidation_withoutToken() {
            // arrange
            val userId = 10L
            whenever(queueHealthChecker.isBypassed()).thenReturn(true)

            // act & assert — 예외 없이 정상 종료
            orderQueueService.validateAndConsumeToken(userId, "any-token")
        }

        @Test
        @DisplayName("잘못된 토큰이어도 검증을 스킵하고 정상 종료한다")
        fun skipsValidation_withWrongToken() {
            // arrange
            val userId = 11L
            whenever(queueHealthChecker.isBypassed()).thenReturn(true)

            // act & assert — Redis 접근 없이 bypass
            orderQueueService.validateAndConsumeToken(userId, "wrong-token")
        }

        @Test
        @DisplayName("순번 조회 시 bypassed=true를 반환한다")
        fun returnsBypassedPosition() {
            // arrange
            val userId = 12L
            whenever(queueHealthChecker.isBypassed()).thenReturn(true)

            // act
            val result = orderQueueService.getPosition(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.bypassed).isTrue() },
                { assertThat(result.token).isNull() },
            )
        }
    }

    @Nested
    @DisplayName("bypass 전환 시나리오,")
    inner class BypassTransition {

        @Test
        @DisplayName("bypass 해제 후 토큰 불일치는 다시 거절된다")
        fun rejectsTokenMismatch_afterBypassEnds() {
            // arrange
            val userId = 20L
            val correctToken = UUID.randomUUID().toString()
            entryTokenRepository.issue(userId, correctToken, 300L)

            // bypass 모드에서 통과
            whenever(queueHealthChecker.isBypassed()).thenReturn(true)
            orderQueueService.validateAndConsumeToken(userId, "any-token")

            // bypass 해제 — 토큰은 소비되지 않았으므로 여전히 존재
            whenever(queueHealthChecker.isBypassed()).thenReturn(false)

            // act — 잘못된 토큰으로 시도
            val exception = assertThrows<CoreException> {
                orderQueueService.validateAndConsumeToken(userId, "wrong-token")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }
}
