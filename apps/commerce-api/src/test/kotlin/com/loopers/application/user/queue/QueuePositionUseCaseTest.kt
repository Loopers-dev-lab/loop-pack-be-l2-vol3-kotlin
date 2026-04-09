package com.loopers.application.user.queue

import com.loopers.domain.queue.EntryToken
import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.QueuePosition
import com.loopers.domain.queue.WaitingQueueRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito.given
import org.mockito.kotlin.mock

@DisplayName("QueuePositionUseCase")
class QueuePositionUseCaseTest {
    private val waitingQueueRepository: WaitingQueueRepository = mock()
    private val entryTokenRepository: EntryTokenRepository = mock()
    private val useCase = QueuePositionUseCase(waitingQueueRepository, entryTokenRepository)

    @Nested
    @DisplayName("순번을 조회한다")
    inner class GetPosition {
        @Test
        @DisplayName("대기 중인 경우 WAITING 상태를 반환한다")
        fun getPosition_waiting() {
            // arrange
            given(entryTokenRepository.findByUserId(1L)).willReturn(null)
            given(waitingQueueRepository.getPosition(1L))
                .willReturn(QueuePosition(position = 50, totalWaiting = 100))

            // act
            val result = useCase.getPosition(QueueCommand.Position(1L))

            // assert
            assertThat(result).isInstanceOf(QueueResult.Position.Waiting::class.java)
            val waiting = result as QueueResult.Position.Waiting
            assertAll(
                { assertThat(waiting.position).isEqualTo(50) },
                { assertThat(waiting.estimatedWaitSeconds).isEqualTo(1) },
                { assertThat(waiting.totalWaiting).isEqualTo(100) },
                { assertThat(waiting.retryAfterMs).isEqualTo(1000) },
            )
        }

        @Test
        @DisplayName("토큰이 발급된 경우 READY 상태를 반환한다")
        fun getPosition_ready() {
            // arrange
            given(entryTokenRepository.findByUserId(1L))
                .willReturn(EntryToken(token = "ready-token", userId = 1L, remainingSeconds = 250))

            // act
            val result = useCase.getPosition(QueueCommand.Position(1L))

            // assert
            assertThat(result).isInstanceOf(QueueResult.Position.Ready::class.java)
            val ready = result as QueueResult.Position.Ready
            assertThat(ready.tokenExpiresInSeconds).isEqualTo(250)
        }

        @Test
        @DisplayName("대기열에 없으면 QUEUE_ENTRY_NOT_FOUND 예외를 던진다")
        fun getPosition_notInQueue() {
            // arrange
            given(entryTokenRepository.findByUserId(999L)).willReturn(null)
            given(waitingQueueRepository.getPosition(999L)).willReturn(null)

            // act & assert
            val exception = assertThrows<CoreException> {
                useCase.getPosition(QueueCommand.Position(999L))
            }
            assertThat(exception.errorType).isEqualTo(ErrorType.QUEUE_ENTRY_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("Polling 주기가 순번에 따라 동적으로 결정된다")
    inner class PollingInterval {
        @Test
        @DisplayName("순번 50 → retryAfterMs == 1000")
        fun getPosition_nearFront() {
            given(entryTokenRepository.findByUserId(1L)).willReturn(null)
            given(waitingQueueRepository.getPosition(1L))
                .willReturn(QueuePosition(position = 50, totalWaiting = 100))

            val result = useCase.getPosition(QueueCommand.Position(1L)) as QueueResult.Position.Waiting
            assertThat(result.retryAfterMs).isEqualTo(1000)
        }

        @Test
        @DisplayName("순번 500 → retryAfterMs == 3000")
        fun getPosition_middle() {
            given(entryTokenRepository.findByUserId(1L)).willReturn(null)
            given(waitingQueueRepository.getPosition(1L))
                .willReturn(QueuePosition(position = 500, totalWaiting = 1000))

            val result = useCase.getPosition(QueueCommand.Position(1L)) as QueueResult.Position.Waiting
            assertThat(result.retryAfterMs).isEqualTo(3000)
        }

        @Test
        @DisplayName("순번 2000 → retryAfterMs == 5000")
        fun getPosition_farBack() {
            given(entryTokenRepository.findByUserId(1L)).willReturn(null)
            given(waitingQueueRepository.getPosition(1L))
                .willReturn(QueuePosition(position = 2000, totalWaiting = 5000))

            val result = useCase.getPosition(QueueCommand.Position(1L)) as QueueResult.Position.Waiting
            assertThat(result.retryAfterMs).isEqualTo(5000)
        }

        @Test
        @DisplayName("순번 100 → retryAfterMs == 1000 (경계값)")
        fun getPosition_boundary100() {
            given(entryTokenRepository.findByUserId(1L)).willReturn(null)
            given(waitingQueueRepository.getPosition(1L))
                .willReturn(QueuePosition(position = 100, totalWaiting = 200))
            val result = useCase.getPosition(QueueCommand.Position(1L)) as QueueResult.Position.Waiting
            assertThat(result.retryAfterMs).isEqualTo(1000)
        }

        @Test
        @DisplayName("순번 101 → retryAfterMs == 3000 (경계값)")
        fun getPosition_boundary101() {
            given(entryTokenRepository.findByUserId(1L)).willReturn(null)
            given(waitingQueueRepository.getPosition(1L))
                .willReturn(QueuePosition(position = 101, totalWaiting = 200))
            val result = useCase.getPosition(QueueCommand.Position(1L)) as QueueResult.Position.Waiting
            assertThat(result.retryAfterMs).isEqualTo(3000)
        }

        @Test
        @DisplayName("순번 1000 → retryAfterMs == 3000 (경계값)")
        fun getPosition_boundary1000() {
            given(entryTokenRepository.findByUserId(1L)).willReturn(null)
            given(waitingQueueRepository.getPosition(1L))
                .willReturn(QueuePosition(position = 1000, totalWaiting = 2000))
            val result = useCase.getPosition(QueueCommand.Position(1L)) as QueueResult.Position.Waiting
            assertThat(result.retryAfterMs).isEqualTo(3000)
        }

        @Test
        @DisplayName("순번 1001 → retryAfterMs == 5000 (경계값)")
        fun getPosition_boundary1001() {
            given(entryTokenRepository.findByUserId(1L)).willReturn(null)
            given(waitingQueueRepository.getPosition(1L))
                .willReturn(QueuePosition(position = 1001, totalWaiting = 2000))
            val result = useCase.getPosition(QueueCommand.Position(1L)) as QueueResult.Position.Waiting
            assertThat(result.retryAfterMs).isEqualTo(5000)
        }
    }
}
