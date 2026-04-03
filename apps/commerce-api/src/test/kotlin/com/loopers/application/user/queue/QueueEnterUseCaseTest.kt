package com.loopers.application.user.queue

import com.loopers.domain.queue.EntryToken
import com.loopers.domain.queue.EntryTokenRepository
import com.loopers.domain.queue.QueuePosition
import com.loopers.domain.queue.WaitingQueueRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.any

@DisplayName("QueueEnterUseCase")
class QueueEnterUseCaseTest {
    private val waitingQueueRepository: WaitingQueueRepository = mock()
    private val entryTokenRepository: EntryTokenRepository = mock()
    private val useCase = QueueEnterUseCase(waitingQueueRepository, entryTokenRepository)

    @Nested
    @DisplayName("대기열에 진입한다")
    inner class Enter {
        @Test
        @DisplayName("첫 진입 시 WAITING 상태와 순번을 반환한다")
        fun enter_success() {
            // arrange
            given(entryTokenRepository.findByUserId(1L)).willReturn(null)
            given(waitingQueueRepository.enter(1L))
                .willReturn(QueuePosition(position = 0, totalWaiting = 1))

            // act
            val result = useCase.enter(QueueCommand.Enter(1L))

            // assert
            assertThat(result).isInstanceOf(QueueResult.Enter.Waiting::class.java)
            val waiting = result as QueueResult.Enter.Waiting
            assertAll(
                { assertThat(waiting.position).isEqualTo(0) },
                { assertThat(waiting.estimatedWaitSeconds).isEqualTo(0) },
                { assertThat(waiting.totalWaiting).isEqualTo(1) },
            )
        }

        @Test
        @DisplayName("이미 토큰이 있으면 READY 상태와 토큰을 반환한다")
        fun enter_alreadyHasToken() {
            // arrange
            given(entryTokenRepository.findByUserId(1L))
                .willReturn(EntryToken(token = "existing-token", userId = 1L, remainingSeconds = 280))

            // act
            val result = useCase.enter(QueueCommand.Enter(1L))

            // assert
            assertThat(result).isInstanceOf(QueueResult.Enter.Ready::class.java)
            val ready = result as QueueResult.Enter.Ready
            assertAll(
                { assertThat(ready.token).isEqualTo("existing-token") },
                { assertThat(ready.tokenExpiresInSeconds).isEqualTo(280) },
            )
            then(waitingQueueRepository).should(never()).enter(any())
        }

        @Test
        @DisplayName("중복 진입 시 기존 순번을 유지한다 (ZADD NX)")
        fun enter_duplicateEntry() {
            // arrange
            given(entryTokenRepository.findByUserId(1L)).willReturn(null)
            given(waitingQueueRepository.enter(1L))
                .willReturn(QueuePosition(position = 5, totalWaiting = 10))

            // act
            val result = useCase.enter(QueueCommand.Enter(1L))

            // assert
            assertThat(result).isInstanceOf(QueueResult.Enter.Waiting::class.java)
            val waiting = result as QueueResult.Enter.Waiting
            assertThat(waiting.position).isEqualTo(5)
        }
    }
}
