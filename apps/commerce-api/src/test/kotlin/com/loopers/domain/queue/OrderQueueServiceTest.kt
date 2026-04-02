package com.loopers.domain.queue

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
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class OrderQueueServiceTest {

    @Mock
    private lateinit var orderQueueRepository: OrderQueueRepository

    @Mock
    private lateinit var entryTokenRepository: EntryTokenRepository

    private lateinit var orderQueueService: OrderQueueService

    @BeforeEach
    fun setUp() {
        orderQueueService = OrderQueueService(orderQueueRepository, entryTokenRepository)
    }

    @Nested
    @DisplayName("대기열 진입할 때,")
    inner class EnterQueue {

        @Test
        @DisplayName("신규 유저이면 true를 반환한다")
        fun returnsTrue_whenNewUser() {
            // arrange
            val userId = 1L
            whenever(orderQueueRepository.enqueue(eq(userId), any())).thenReturn(true)

            // act
            val result = orderQueueService.enterQueue(userId)

            // assert
            assertThat(result).isTrue()
        }

        @Test
        @DisplayName("이미 대기열에 있는 유저이면 false를 반환한다")
        fun returnsFalse_whenAlreadyInQueue() {
            // arrange
            val userId = 1L
            whenever(orderQueueRepository.enqueue(eq(userId), any())).thenReturn(false)

            // act
            val result = orderQueueService.enterQueue(userId)

            // assert
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("대기열 순번 조회할 때,")
    inner class GetPosition {

        @Test
        @DisplayName("대기열에 있으면 순번과 전체 대기 인원을 반환한다")
        fun returnsQueuePosition_whenInQueue() {
            // arrange
            val userId = 1L
            whenever(orderQueueRepository.getPosition(userId)).thenReturn(5L)
            whenever(orderQueueRepository.getTotalSize()).thenReturn(100L)
            whenever(entryTokenRepository.get(userId)).thenReturn(null)

            // act
            val result = orderQueueService.getPosition(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(5L) },
                { assertThat(result.totalSize).isEqualTo(100L) },
                { assertThat(result.token).isNull() },
            )
        }

        @Test
        @DisplayName("대기열에 없는 유저이면 NOT_FOUND 예외가 발생한다")
        fun throwsNotFound_whenUserNotInQueue() {
            // arrange
            val userId = 999L
            whenever(entryTokenRepository.get(userId)).thenReturn(null)
            whenever(orderQueueRepository.getPosition(userId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                orderQueueService.getPosition(userId)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("예상 대기 시간은 position / 175.0 (초)로 계산된다")
        fun calculatesEstimatedWaitSeconds() {
            // arrange
            val userId = 1L
            whenever(orderQueueRepository.getPosition(userId)).thenReturn(350L)
            whenever(orderQueueRepository.getTotalSize()).thenReturn(500L)
            whenever(entryTokenRepository.get(userId)).thenReturn(null)

            // act
            val result = orderQueueService.getPosition(userId)

            // assert
            assertThat(result.estimatedWaitSeconds).isEqualTo(2.0)
        }

        @Test
        @DisplayName("토큰이 이미 발급되었으면 position=0과 token을 반환한다")
        fun returnsPositionZeroWithToken_whenTokenAlreadyIssued() {
            // arrange
            val userId = 1L
            val token = "issued-token"
            whenever(entryTokenRepository.get(userId)).thenReturn(token)

            // act
            val result = orderQueueService.getPosition(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.token).isEqualTo(token) },
            )
        }
    }

    @Nested
    @DisplayName("유저 입장 처리할 때,")
    inner class AdmitUsers {

        @Test
        @DisplayName("N명을 pop하고 각각 토큰을 발급하여 발급 인원 수를 반환한다")
        fun returnsAdmittedCount_whenUsersPopped() {
            // arrange
            val batchSize = 3L
            whenever(orderQueueRepository.popFront(batchSize)).thenReturn(listOf(1L, 2L, 3L))

            // act
            val result = orderQueueService.admitUsers(batchSize)

            // assert
            assertThat(result).isEqualTo(3L)
        }

        @Test
        @DisplayName("대기열이 비어있으면 0을 반환한다")
        fun returnsZero_whenQueueIsEmpty() {
            // arrange
            val batchSize = 5L
            whenever(orderQueueRepository.popFront(batchSize)).thenReturn(emptyList())

            // act
            val result = orderQueueService.admitUsers(batchSize)

            // assert
            assertThat(result).isEqualTo(0L)
        }

        @Test
        @DisplayName("popFront로 유저를 꺼내고 각각에게 토큰을 발급한다")
        fun callsPopFrontAndIssueForEachUser() {
            // arrange
            val batchSize = 2L
            whenever(orderQueueRepository.popFront(batchSize)).thenReturn(listOf(10L, 20L))

            // act
            orderQueueService.admitUsers(batchSize)

            // assert
            verify(orderQueueRepository).popFront(batchSize)
            verify(entryTokenRepository).issue(eq(10L), any(), any())
            verify(entryTokenRepository).issue(eq(20L), any(), any())
            verify(entryTokenRepository, times(2)).issue(any(), any(), any())
        }
    }

    @Nested
    @DisplayName("토큰 검증 및 소비할 때,")
    inner class ValidateAndConsumeToken {

        @Test
        @DisplayName("유효한 토큰이면 소비하고 정상 종료한다")
        fun consumesToken_whenValid() {
            // arrange
            val userId = 1L
            val token = "valid-token"
            whenever(entryTokenRepository.get(userId)).thenReturn(token)

            // act & assert — 예외 없이 정상 종료
            orderQueueService.validateAndConsumeToken(userId, token)
        }

        @Test
        @DisplayName("만료/존재하지 않는 토큰이면 FORBIDDEN 예외가 발생한다")
        fun throwsForbidden_whenTokenNotExists() {
            // arrange
            val userId = 1L
            whenever(entryTokenRepository.get(userId)).thenReturn(null)

            // act
            val exception = assertThrows<CoreException> {
                orderQueueService.validateAndConsumeToken(userId, "any-token")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @Test
        @DisplayName("잘못된 토큰 값이면 FORBIDDEN 예외가 발생한다")
        fun throwsForbidden_whenTokenMismatch() {
            // arrange
            val userId = 1L
            whenever(entryTokenRepository.get(userId)).thenReturn("correct-token")

            // act
            val exception = assertThrows<CoreException> {
                orderQueueService.validateAndConsumeToken(userId, "wrong-token")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }
    }
}
