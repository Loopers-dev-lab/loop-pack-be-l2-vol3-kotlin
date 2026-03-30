package com.loopers.interfaces.support.scheduler

import com.loopers.application.queue.IssueEntryTokensUseCase
import com.loopers.application.queue.QueueProperties
import com.loopers.domain.common.vo.UserId
import com.loopers.domain.queue.token.FakeEntryTokenRepository
import com.loopers.domain.queue.waiting.FakeWaitingQueueRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueueSchedulerTest {

    private lateinit var waitingQueueRepository: FakeWaitingQueueRepository
    private lateinit var entryTokenRepository: FakeEntryTokenRepository
    private lateinit var scheduler: QueueScheduler

    @BeforeEach
    fun setUp() {
        waitingQueueRepository = FakeWaitingQueueRepository()
        entryTokenRepository = FakeEntryTokenRepository()
        val useCase = IssueEntryTokensUseCase(
            waitingQueueRepository = waitingQueueRepository,
            entryTokenRepository = entryTokenRepository,
            queueProperties = QueueProperties(
                maxCapacity = 50_000,
                batchSize = 3,
                tokenTtlSeconds = 300,
                throughputTps = 175,
                schedulerDelayMs = 100,
                jitterMaxMs = 0,
            ),
        )
        scheduler = QueueScheduler(useCase)
    }

    @Nested
    @DisplayName("issueTokens 실행 시")
    inner class IssueTokens {

        @Test
        @DisplayName("대기열에서 배치 크기만큼 꺼내 토큰을 발급한다")
        fun issueTokens_popsAndIssuesTokens() {
            // arrange
            waitingQueueRepository.enter(1L, 1000.0, 50_000)
            waitingQueueRepository.enter(2L, 2000.0, 50_000)

            // act
            scheduler.issueTokens()

            // assert
            assertThat(entryTokenRepository.find(UserId(1L))).isNotNull()
            assertThat(entryTokenRepository.find(UserId(2L))).isNotNull()
            assertThat(waitingQueueRepository.count()).isEqualTo(0)
        }

        @Test
        @DisplayName("대기열이 비어있으면 아무 동작도 하지 않는다")
        fun issueTokens_emptyQueue_doesNothing() {
            // act
            scheduler.issueTokens()

            // assert
            assertThat(waitingQueueRepository.count()).isEqualTo(0)
        }
    }
}
