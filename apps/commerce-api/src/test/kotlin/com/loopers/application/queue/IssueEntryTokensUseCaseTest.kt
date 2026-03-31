package com.loopers.application.queue

import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.domain.queue.waiting.FakeWaitingQueueRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class IssueEntryTokensUseCaseTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var issueEntryTokensUseCase: IssueEntryTokensUseCase

    private val batchSize = 3

    @BeforeEach
    fun setUp() {
        waitingQueueRepository = FakeWaitingQueueRepository()
        entryTokenRepository = FakeEntryTokenRepository()
        issueEntryTokensUseCase = IssueEntryTokensUseCase(
            waitingQueueRepository = waitingQueueRepository,
            entryTokenRepository = entryTokenRepository,
            queueProperties = QueueProperties(
                maxCapacity = 50_000,
                batchSize = batchSize,
                tokenTtlSeconds = 300,
                throughputTps = 175,
                schedulerDelayMs = 100,
                jitterMaxMs = 0,
            ),
        )
    }

    @Nested
    @DisplayName("execute 시")
    inner class Execute {

        @Test
        @DisplayName("대기열에서 batchSize만큼 꺼내 각각 토큰을 발급한다")
        fun execute_popsAndIssuesTokens() {
            // arrange
            waitingQueueRepository.enter(1L, 1000.0, 50_000)
            waitingQueueRepository.enter(2L, 2000.0, 50_000)
            waitingQueueRepository.enter(3L, 3000.0, 50_000)

            // act
            val result = issueEntryTokensUseCase.execute()

            // assert
            assertThat(result).hasSize(3)
            assertThat(result.map { it.userId }).containsExactly(1L, 2L, 3L)
            result.forEach { assertThat(it.token).isNotBlank() }
            assertThat(entryTokenRepository.find(UserId(1L))).isNotNull()
            assertThat(entryTokenRepository.find(UserId(2L))).isNotNull()
            assertThat(entryTokenRepository.find(UserId(3L))).isNotNull()
            assertThat(waitingQueueRepository.count()).isEqualTo(0)
        }

        @Test
        @DisplayName("대기열이 비어있으면 빈 리스트를 반환한다")
        fun execute_emptyQueue_returnsEmptyList() {
            // act
            val result = issueEntryTokensUseCase.execute()

            // assert
            assertThat(result).isEmpty()
            assertThat(waitingQueueRepository.count()).isEqualTo(0)
        }

        @Test
        @DisplayName("대기열 인원이 batchSize보다 적으면 있는 만큼만 발급한다")
        fun execute_lessThanBatchSize_issuesOnlyAvailable() {
            // arrange
            waitingQueueRepository.enter(1L, 1000.0, 50_000)

            // act
            issueEntryTokensUseCase.execute()

            // assert
            assertThat(entryTokenRepository.find(UserId(1L))).isNotNull()
            assertThat(waitingQueueRepository.count()).isEqualTo(0)
        }

        @Test
        @DisplayName("jitter가 설정되어 있어도 모든 토큰이 정상 발급된다")
        fun execute_withJitter_issuesTokensCorrectly() {
            // arrange
            val jitterUseCase = IssueEntryTokensUseCase(
                waitingQueueRepository = waitingQueueRepository,
                entryTokenRepository = entryTokenRepository,
                queueProperties = QueueProperties(
                    maxCapacity = 50_000,
                    batchSize = batchSize,
                    tokenTtlSeconds = 300,
                    throughputTps = 175,
                    schedulerDelayMs = 100,
                    jitterMaxMs = 5,
                ),
            )
            waitingQueueRepository.enter(1L, 1000.0, 50_000)
            waitingQueueRepository.enter(2L, 2000.0, 50_000)
            waitingQueueRepository.enter(3L, 3000.0, 50_000)

            // act
            jitterUseCase.execute()

            // assert
            assertThat(entryTokenRepository.find(UserId(1L))).isNotNull()
            assertThat(entryTokenRepository.find(UserId(2L))).isNotNull()
            assertThat(entryTokenRepository.find(UserId(3L))).isNotNull()
            assertThat(waitingQueueRepository.count()).isEqualTo(0)
        }

        @Test
        @DisplayName("batchSize를 초과하는 인원은 대기열에 남는다")
        fun execute_moreThanBatchSize_remainsInQueue() {
            // arrange
            waitingQueueRepository.enter(1L, 1000.0, 50_000)
            waitingQueueRepository.enter(2L, 2000.0, 50_000)
            waitingQueueRepository.enter(3L, 3000.0, 50_000)
            waitingQueueRepository.enter(4L, 4000.0, 50_000)
            waitingQueueRepository.enter(5L, 5000.0, 50_000)

            // act
            issueEntryTokensUseCase.execute()

            // assert — batchSize=3이므로 3명만 발급, 2명은 대기열에 남음
            assertThat(entryTokenRepository.find(UserId(1L))).isNotNull()
            assertThat(entryTokenRepository.find(UserId(2L))).isNotNull()
            assertThat(entryTokenRepository.find(UserId(3L))).isNotNull()
            assertThat(entryTokenRepository.find(UserId(4L))).isNull()
            assertThat(entryTokenRepository.find(UserId(5L))).isNull()
            assertThat(waitingQueueRepository.count()).isEqualTo(2)
        }
    }
}
