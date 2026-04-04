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

    @Mock
    private lateinit var queueHealthChecker: QueueHealthChecker

    private lateinit var orderQueueService: OrderQueueService

    @BeforeEach
    fun setUp() {
        orderQueueService = OrderQueueService(orderQueueRepository, entryTokenRepository, queueHealthChecker)
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
                { assertThat(result.bypassed).isFalse() },
            )
        }

        @Test
        @DisplayName("bypass 모드이면 position=0, bypassed=true를 반환한다")
        fun returnsBypassedPosition_whenBypassed() {
            // arrange
            val userId = 1L
            whenever(queueHealthChecker.isBypassed()).thenReturn(true)

            // act
            val result = orderQueueService.getPosition(userId)

            // assert
            assertAll(
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.bypassed).isTrue() },
                { assertThat(result.token).isNull() },
            )
            // Redis 접근 없이 즉시 반환
            verify(entryTokenRepository, times(0)).get(userId)
        }
    }

    @Nested
    @DisplayName("대기 중인 유저 포지션 일괄 조회할 때,")
    inner class GetWaitingPositions {

        @Test
        @DisplayName("totalSize를 1회만 조회하고 각 유저의 포지션을 반환한다")
        fun returnsBatchPositions_withSingleTotalSizeQuery() {
            // arrange
            whenever(orderQueueRepository.getTotalSize()).thenReturn(200L)
            whenever(orderQueueRepository.getPosition(1L)).thenReturn(3L)
            whenever(orderQueueRepository.getPosition(2L)).thenReturn(7L)

            // act
            val result = orderQueueService.getWaitingPositions(listOf(1L, 2L))

            // assert
            assertAll(
                { assertThat(result).hasSize(2) },
                { assertThat(result[1L]?.position).isEqualTo(3L) },
                { assertThat(result[2L]?.position).isEqualTo(7L) },
                { assertThat(result[1L]?.totalSize).isEqualTo(200L) },
            )
            verify(orderQueueRepository, times(1)).getTotalSize()
        }

        @Test
        @DisplayName("대기열에 없는 유저는 결과에서 제외된다")
        fun excludesUsersNotInQueue() {
            // arrange
            whenever(orderQueueRepository.getTotalSize()).thenReturn(100L)
            whenever(orderQueueRepository.getPosition(1L)).thenReturn(null)
            whenever(orderQueueRepository.getPosition(2L)).thenReturn(5L)

            // act
            val result = orderQueueService.getWaitingPositions(listOf(1L, 2L))

            // assert
            assertThat(result).hasSize(1)
            assertThat(result).containsKey(2L)
        }

        @Test
        @DisplayName("빈 목록이면 빈 맵을 반환한다")
        fun returnsEmptyMap_whenEmpty() {
            // act
            val result = orderQueueService.getWaitingPositions(emptyList())

            // assert
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("유저 입장 처리할 때,")
    inner class AdmitUsers {

        @Test
        @DisplayName("N명을 pop하고 각각 토큰을 발급하여 입장 유저 목록을 반환한다")
        fun returnsAdmittedUserIds_whenUsersPopped() {
            // arrange
            val batchSize = 3L
            whenever(orderQueueRepository.popFront(batchSize)).thenReturn(listOf(1L, 2L, 3L))
            whenever(entryTokenRepository.issue(any(), any(), any())).thenReturn(true)

            // act
            val result = orderQueueService.admitUsers(batchSize)

            // assert
            assertThat(result).containsExactly(1L, 2L, 3L)
        }

        @Test
        @DisplayName("대기열이 비어있으면 빈 목록을 반환한다")
        fun returnsEmptyList_whenQueueIsEmpty() {
            // arrange
            val batchSize = 5L
            whenever(orderQueueRepository.popFront(batchSize)).thenReturn(emptyList())

            // act
            val result = orderQueueService.admitUsers(batchSize)

            // assert
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("popFront로 유저를 꺼내고 각각에게 토큰을 발급한다")
        fun callsPopFrontAndIssueForEachUser() {
            // arrange
            val batchSize = 2L
            whenever(orderQueueRepository.popFront(batchSize)).thenReturn(listOf(10L, 20L))
            whenever(entryTokenRepository.issue(any(), any(), any())).thenReturn(true)

            // act
            orderQueueService.admitUsers(batchSize)

            // assert
            verify(orderQueueRepository).popFront(batchSize)
            verify(entryTokenRepository).issue(eq(10L), any(), any())
            verify(entryTokenRepository).issue(eq(20L), any(), any())
            verify(entryTokenRepository, times(2)).issue(any(), any(), any())
        }

        @Test
        @DisplayName("토큰 발급에 실패한 유저는 승인 목록에서 제외되고 대기열에 재삽입된다")
        fun requeueFailedUsers_whenTokenIssueFails() {
            // arrange
            val batchSize = 3L
            whenever(orderQueueRepository.popFront(batchSize)).thenReturn(listOf(1L, 2L, 3L))
            whenever(entryTokenRepository.issue(eq(1L), any(), any())).thenReturn(true)
            whenever(entryTokenRepository.issue(eq(2L), any(), any())).thenReturn(false)
            whenever(entryTokenRepository.issue(eq(3L), any(), any())).thenReturn(true)

            // act
            val result = orderQueueService.admitUsers(batchSize)

            // assert
            assertAll(
                { assertThat(result).containsExactly(1L, 3L) },
                { verify(orderQueueRepository).requeue(listOf(2L)) },
            )
        }

        @Test
        @DisplayName("모든 유저의 토큰 발급이 실패하면 빈 목록을 반환하고 전원 재삽입한다")
        fun requeueAllUsers_whenAllTokenIssuesFail() {
            // arrange
            val batchSize = 2L
            whenever(orderQueueRepository.popFront(batchSize)).thenReturn(listOf(1L, 2L))
            whenever(entryTokenRepository.issue(any(), any(), any())).thenReturn(false)

            // act
            val result = orderQueueService.admitUsers(batchSize)

            // assert
            assertAll(
                { assertThat(result).isEmpty() },
                { verify(orderQueueRepository).requeue(listOf(1L, 2L)) },
            )
        }

        @Test
        @DisplayName("모든 유저의 토큰 발급이 성공하면 requeue를 호출하지 않는다")
        fun doesNotRequeue_whenAllTokenIssuesSucceed() {
            // arrange
            val batchSize = 2L
            whenever(orderQueueRepository.popFront(batchSize)).thenReturn(listOf(1L, 2L))
            whenever(entryTokenRepository.issue(any(), any(), any())).thenReturn(true)

            // act
            orderQueueService.admitUsers(batchSize)

            // assert
            verify(orderQueueRepository, times(0)).requeue(any())
        }
    }

    @Nested
    @DisplayName("토큰 검증 및 소비할 때,")
    inner class ValidateAndConsumeToken {

        @Test
        @DisplayName("유효한 토큰이면 원자적으로 소비하고 정상 종료한다")
        fun consumesToken_whenValid() {
            // arrange
            val userId = 1L
            val token = "valid-token"
            whenever(entryTokenRepository.consumeIfMatches(userId, token)).thenReturn(true)

            // act & assert — 예외 없이 정상 종료
            orderQueueService.validateAndConsumeToken(userId, token)

            // assert
            verify(entryTokenRepository).consumeIfMatches(userId, token)
        }

        @Test
        @DisplayName("토큰이 유효하지 않으면 FORBIDDEN 예외가 발생한다")
        fun throwsForbidden_whenTokenInvalid() {
            // arrange
            val userId = 1L
            whenever(entryTokenRepository.consumeIfMatches(userId, "wrong-token")).thenReturn(false)

            // act
            val exception = assertThrows<CoreException> {
                orderQueueService.validateAndConsumeToken(userId, "wrong-token")
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.FORBIDDEN)
        }

        @Test
        @DisplayName("bypass 모드이면 토큰 검증을 스킵하고 정상 종료한다")
        fun skipsValidation_whenBypassed() {
            // arrange
            val userId = 1L
            whenever(queueHealthChecker.isBypassed()).thenReturn(true)

            // act & assert — 예외 없이 정상 종료
            orderQueueService.validateAndConsumeToken(userId, "any-token")

            // assert — Redis 접근 없이 즉시 반환
            verify(entryTokenRepository, times(0)).consumeIfMatches(any(), any())
        }
    }
}
