package com.loopers.infrastructure.scheduler

import com.loopers.domain.queue.QueueRepository
import com.loopers.domain.queue.QueuedUser
import com.loopers.infrastructure.queue.WaitingQueueRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueueSchedulerFailureTest {

    private lateinit var queueScheduler: QueueScheduler
    private lateinit var queueRepository: QueueRepository
    private lateinit var waitingQueueRegistry: WaitingQueueRegistry

    companion object {
        private const val QUEUE_NAME = "test-queue"
    }

    @BeforeEach
    fun setUp() {
        queueRepository = mockk()
        waitingQueueRegistry = mockk()
        queueScheduler = QueueScheduler(queueRepository, waitingQueueRegistry)
    }

    @DisplayName("토큰 TTL 검증")
    @Nested
    inner class TokenTtlTest {

        @Test
        fun `큐 설정의 TTL(300초)로 토큰이 발급된다`() {
            // arrange
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 175,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)
            every { queueRepository.popMin(QUEUE_NAME, any()) } returns listOf(QueuedUser(1L, 1.0))
            every { queueRepository.issueToken(QUEUE_NAME, 1L, any(), 300L) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert
            verify { queueRepository.issueToken(QUEUE_NAME, 1L, any(), 300L) }
        }
    }

    @DisplayName("배치 크기 계산 - 엣지 케이스")
    @Nested
    inner class BatchSizeCalculationTest {

        private fun batchSizeFor(throughput: Int): Long = maxOf(1L, (throughput / 10).toLong())

        @Test
        fun `throughput이 5일 때 batchSize는 1이다`() {
            assertThat(batchSizeFor(5)).isEqualTo(1L)
        }

        @Test
        fun `throughput이 0일 때 batchSize는 1이다`() {
            assertThat(batchSizeFor(0)).isEqualTo(1L)
        }

        @Test
        fun `throughput이 175일 때 batchSize는 17이다`() {
            assertThat(batchSizeFor(175)).isEqualTo(17L)
        }
    }

    @DisplayName("토큰 발급 부분 실패 - Fail-Fast 검증")
    @Nested
    inner class PartialTokenIssueFailureTest {

        @Test
        fun `배치 중 일부 사용자만 토큰 발급 실패하면 실패한 것만 에러 로그된다`() {
            // arrange
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 175,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)
            every { queueRepository.popMin(QUEUE_NAME, 17L) } returns listOf(QueuedUser(1L, 1.0), QueuedUser(2L, 2.0), QueuedUser(3L, 3.0))
            every { queueRepository.issueToken(QUEUE_NAME, 1L, any(), any()) } returns Unit
            every { queueRepository.issueToken(QUEUE_NAME, 2L, any(), any()) } throws RuntimeException("Redis 오류")
            every { queueRepository.issueToken(QUEUE_NAME, 3L, any(), any()) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert: user-1, 3는 성공, user-2만 실패
            verify { queueRepository.issueToken(QUEUE_NAME, 1L, any(), any()) }
            verify { queueRepository.issueToken(QUEUE_NAME, 2L, any(), any()) }
            verify { queueRepository.issueToken(QUEUE_NAME, 3L, any(), any()) }
        }
    }
}
