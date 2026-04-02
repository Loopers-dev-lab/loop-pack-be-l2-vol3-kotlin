package com.loopers.application.queue

import com.loopers.infrastructure.queue.WaitingQueueRedisRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class QueueServiceTest {

    @Mock
    private lateinit var waitingQueueRedisRepository: WaitingQueueRedisRepository

    private lateinit var queueService: QueueService

    @BeforeEach
    fun setUp() {
        val properties = QueueProperties(fallbackStrategy = QueueFallbackStrategy.BLOCK)
        queueService = QueueService(waitingQueueRedisRepository, properties)
    }

    @DisplayName("대기열에 진입할 때,")
    @Nested
    inner class EnterQueue {

        @DisplayName("신규 유저이면, 순번과 예상 대기시간을 반환한다.")
        @Test
        fun returnsPositionAndWaitTime_whenNewUser() {
            // arrange
            val userId = 1L
            whenever(waitingQueueRedisRepository.hasToken(userId)).thenReturn(false)
            whenever(waitingQueueRedisRepository.addToQueue(userId)).thenReturn(100L)
            whenever(waitingQueueRedisRepository.getTotalCount()).thenReturn(500L)

            // act
            val result = queueService.enterQueue(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(100L) },
                { assertThat(result.estimatedWaitSeconds).isEqualTo(0L) },
                { assertThat(result.totalWaiting).isEqualTo(500L) },
            )
        }

        @DisplayName("이미 토큰을 보유한 유저이면, position=0을 반환한다.")
        @Test
        fun returnsZeroPosition_whenAlreadyHasToken() {
            // arrange
            val userId = 1L
            whenever(waitingQueueRedisRepository.hasToken(userId)).thenReturn(true)
            whenever(waitingQueueRedisRepository.getTotalCount()).thenReturn(500L)

            // act
            val result = queueService.enterQueue(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.estimatedWaitSeconds).isEqualTo(0L) },
            )
        }

        @DisplayName("순번이 175 이상이면, 예상 대기시간이 1초 이상이다.")
        @Test
        fun returnsWaitTimeOverOneSecond_whenPositionIsHigh() {
            // arrange
            val userId = 1L
            whenever(waitingQueueRedisRepository.hasToken(userId)).thenReturn(false)
            whenever(waitingQueueRedisRepository.addToQueue(userId)).thenReturn(350L)
            whenever(waitingQueueRedisRepository.getTotalCount()).thenReturn(500L)

            // act
            val result = queueService.enterQueue(userId)

            // assert
            assertThat(result.estimatedWaitSeconds).isEqualTo(2L)
        }
    }

    @DisplayName("순번을 조회할 때,")
    @Nested
    inner class GetQueuePosition {

        @DisplayName("토큰이 발급된 유저이면, position=0과 토큰을 반환한다.")
        @Test
        fun returnsTokenWithZeroPosition_whenTokenExists() {
            // arrange
            val userId = 1L
            whenever(waitingQueueRedisRepository.getToken(userId)).thenReturn("test-token")
            whenever(waitingQueueRedisRepository.getTotalCount()).thenReturn(500L)

            // act
            val result = queueService.getQueuePosition(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.token).isEqualTo("test-token") },
            )
        }

        @DisplayName("대기열에 있는 유저이면, 순번과 예상 대기시간을 반환한다.")
        @Test
        fun returnsPosition_whenInQueue() {
            // arrange
            val userId = 1L
            whenever(waitingQueueRedisRepository.getToken(userId)).thenReturn(null)
            whenever(waitingQueueRedisRepository.getPosition(userId)).thenReturn(50L)
            whenever(waitingQueueRedisRepository.getTotalCount()).thenReturn(500L)

            // act
            val result = queueService.getQueuePosition(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(50L) },
                { assertThat(result.token).isNull() },
            )
        }

        @DisplayName("대기열에도 없고 토큰도 없는 유저이면, position=0과 token=null을 반환한다.")
        @Test
        fun returnsZeroWithNoToken_whenNotInQueueAndNoToken() {
            // arrange
            val userId = 1L
            whenever(waitingQueueRedisRepository.getToken(userId)).thenReturn(null)
            whenever(waitingQueueRedisRepository.getPosition(userId)).thenReturn(null)
            whenever(waitingQueueRedisRepository.getTotalCount()).thenReturn(0L)

            // act
            val result = queueService.getQueuePosition(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.token).isNull() },
            )
        }
    }

    @DisplayName("토큰을 검증할 때,")
    @Nested
    inner class ValidateAndConsumeToken {

        @DisplayName("토큰이 존재하면, 예외가 발생하지 않는다.")
        @Test
        fun doesNotThrow_whenTokenExists() {
            // arrange
            val userId = 1L
            whenever(waitingQueueRedisRepository.getToken(userId)).thenReturn("valid-token")

            // act & assert
            queueService.validateAndConsumeToken(userId)
        }

        @DisplayName("토큰이 없으면, FORBIDDEN 예외가 발생한다.")
        @Test
        fun throwsForbidden_whenTokenNotFound() {
            // arrange
            val userId = 1L
            whenever(waitingQueueRedisRepository.getToken(userId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                queueService.validateAndConsumeToken(userId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }

    @DisplayName("Redis 장애 시 BLOCK 전략이면,")
    @Nested
    inner class FallbackBlock {

        @DisplayName("대기열 진입 시 SERVICE_UNAVAILABLE 예외가 발생한다.")
        @Test
        fun throwsServiceUnavailable_whenEnterQueueFails() {
            // arrange
            whenever(waitingQueueRedisRepository.hasToken(1L))
                .thenThrow(RuntimeException("Redis connection refused"))

            // act
            val exception = assertThrows<CoreException> {
                queueService.enterQueue(1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
        }

        @DisplayName("순번 조회 시 SERVICE_UNAVAILABLE 예외가 발생한다.")
        @Test
        fun throwsServiceUnavailable_whenGetPositionFails() {
            // arrange
            whenever(waitingQueueRedisRepository.getToken(1L))
                .thenThrow(RuntimeException("Redis connection refused"))

            // act
            val exception = assertThrows<CoreException> {
                queueService.getQueuePosition(1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
        }

        @DisplayName("토큰 검증 시 SERVICE_UNAVAILABLE 예외가 발생한다.")
        @Test
        fun throwsServiceUnavailable_whenValidateTokenFails() {
            // arrange
            whenever(waitingQueueRedisRepository.getToken(1L))
                .thenThrow(RuntimeException("Redis connection refused"))

            // act
            val exception = assertThrows<CoreException> {
                queueService.validateAndConsumeToken(1L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
        }
    }

    @DisplayName("Redis 장애 시 BYPASS 전략이면,")
    @Nested
    inner class FallbackBypass {

        private lateinit var bypassQueueService: QueueService

        @BeforeEach
        fun setUp() {
            val properties = QueueProperties(fallbackStrategy = QueueFallbackStrategy.BYPASS)
            bypassQueueService = QueueService(waitingQueueRedisRepository, properties)
        }

        @DisplayName("대기열 진입 시 position=0으로 즉시 통과한다.")
        @Test
        fun returnsBypassEntry_whenEnterQueueFails() {
            // arrange
            whenever(waitingQueueRedisRepository.hasToken(1L))
                .thenThrow(RuntimeException("Redis connection refused"))

            // act
            val result = bypassQueueService.enterQueue(1L)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.estimatedWaitSeconds).isEqualTo(0L) },
                { assertThat(result.totalWaiting).isEqualTo(0L) },
            )
        }

        @DisplayName("순번 조회 시 BYPASS 토큰을 즉시 반환한다.")
        @Test
        fun returnsBypassToken_whenGetPositionFails() {
            // arrange
            whenever(waitingQueueRedisRepository.getToken(1L))
                .thenThrow(RuntimeException("Redis connection refused"))

            // act
            val result = bypassQueueService.getQueuePosition(1L)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.token).isEqualTo("BYPASS") },
                { assertThat(result.nextPollAfterMs).isEqualTo(0L) },
                { assertThat(result.activateAfterMs).isEqualTo(0L) },
            )
        }

        @DisplayName("토큰 검증 시 예외 없이 통과한다.")
        @Test
        fun passesValidation_whenValidateTokenFails() {
            // arrange
            whenever(waitingQueueRedisRepository.getToken(1L))
                .thenThrow(RuntimeException("Redis connection refused"))

            // act & assert (예외 없이 통과)
            bypassQueueService.validateAndConsumeToken(1L)
        }
    }

    @DisplayName("토큰 발급 지표를 관리할 때,")
    @Nested
    inner class TokenMetrics {

        @DisplayName("토큰을 발급하면 발급 카운트가 증가한다.")
        @Test
        fun incrementsIssuedCount_whenTokensIssued() {
            // arrange
            whenever(waitingQueueRedisRepository.popAndIssueTokens(18L))
                .thenReturn(listOf(1L, 2L, 3L))

            // act
            queueService.issueTokens(18L)

            // assert
            assertThat(queueService.getTokenIssuedCount()).isEqualTo(3L)
        }

        @DisplayName("만료율을 정확히 계산한다.")
        @Test
        fun calculatesExpiryRate_whenSomeTokensConsumed() {
            // arrange
            whenever(waitingQueueRedisRepository.popAndIssueTokens(18L))
                .thenReturn(listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L))
            queueService.issueTokens(18L)

            // 10명 발급, 7명 소비
            repeat(7) { queueService.incrementConsumedCount() }

            // act
            val expiryRate = queueService.getTokenExpiryRate()

            // assert
            assertThat(expiryRate).isEqualTo(0.3)
        }

        @DisplayName("발급이 0건이면 만료율은 0.0이다.")
        @Test
        fun returnsZero_whenNoTokensIssued() {
            // act
            val expiryRate = queueService.getTokenExpiryRate()

            // assert
            assertThat(expiryRate).isEqualTo(0.0)
        }
    }
}
