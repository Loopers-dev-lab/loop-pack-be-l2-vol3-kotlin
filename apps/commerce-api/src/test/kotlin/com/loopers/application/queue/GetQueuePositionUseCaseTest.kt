package com.loopers.application.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.domain.queue.waiting.FakeWaitingQueueRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetQueuePositionUseCaseTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var getQueuePositionUseCase: GetQueuePositionUseCase

    private val maxCapacity = 50_000
    private val throughputTps = 175

    @BeforeEach
    fun setUp() {
        waitingQueueRepository = FakeWaitingQueueRepository()
        entryTokenRepository = FakeEntryTokenRepository()
        getQueuePositionUseCase = GetQueuePositionUseCase(
            waitingQueueRepository = waitingQueueRepository,
            entryTokenRepository = entryTokenRepository,
            queueProperties = QueueProperties(
                maxCapacity = maxCapacity,
                batchSize = 18,
                tokenTtlSeconds = 300,
                throughputTps = throughputTps,
                schedulerDelayMs = 100,
            ),
        )
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("대기열에 없는 유저 조회 시 NOT_FOUND 예외가 발생한다")
        fun execute_notInQueue_throwsNotFound() {
            // act
            val exception = assertThrows<CoreException> {
                getQueuePositionUseCase.execute(999L)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @Test
        @DisplayName("토큰 보유 시 토큰 정보를 포함하여 반환한다")
        fun execute_withToken_returnsTokenInfo() {
            // arrange
            entryTokenRepository.issue(UserId(1L), "issued-token", 300)

            // act
            val result = getQueuePositionUseCase.execute(1L)

            // assert
            assertThat(result.token).isEqualTo("issued-token")
            assertThat(result.position).isEqualTo(0)
            assertThat(result.estimatedWaitSeconds).isEqualTo(0)
        }

        @Test
        @DisplayName("정상 조회 시 순번과 예상 대기 시간을 반환한다")
        fun execute_inQueue_returnsPositionAndEstimatedWait() {
            // arrange
            waitingQueueRepository.enter(1L, 1000.0, maxCapacity)
            waitingQueueRepository.enter(2L, 2000.0, maxCapacity)
            waitingQueueRepository.enter(3L, 3000.0, maxCapacity)

            // act
            val result = getQueuePositionUseCase.execute(3L)

            // assert
            assertThat(result.position).isEqualTo(2L)
            assertThat(result.estimatedWaitSeconds).isEqualTo(2L / throughputTps)
            assertThat(result.token).isNull()
        }
    }
}
