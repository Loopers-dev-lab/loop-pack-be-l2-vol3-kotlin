package com.loopers.infrastructure.scheduler

import com.loopers.domain.queue.QueueRepository
import com.loopers.domain.queue.QueuedUser
import com.loopers.infrastructure.queue.WaitingQueueRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueueSchedulerMultiQueueTest {

    private lateinit var queueScheduler: QueueScheduler
    private lateinit var queueRepository: QueueRepository
    private lateinit var waitingQueueRegistry: WaitingQueueRegistry

    companion object {
        private const val QUEUE_A = "order-queue"
        private const val QUEUE_B = "coupon-queue"
    }

    @BeforeEach
    fun setUp() {
        queueRepository = mockk()
        waitingQueueRegistry = mockk()
        queueScheduler = QueueScheduler(queueRepository, waitingQueueRegistry)
    }

    @DisplayName("여러 큐 격리 - Fail-Fast 검증")
    @Nested
    inner class MultiQueueIsolationTest {

        @Test
        fun `큐 A 처리 중 예외 발생해도 큐 B는 정상 처리된다`() {
            // arrange
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(
                WaitingQueueRegistry.QueueConfig(name = QUEUE_A, throughputPerServerPerSecond = 175, activeTokenTTLSeconds = 300),
                WaitingQueueRegistry.QueueConfig(name = QUEUE_B, throughputPerServerPerSecond = 100, activeTokenTTLSeconds = 300),
            )
            every { queueRepository.popMin(QUEUE_A, any()) } throws RuntimeException("큐 A 오류")
            every { queueRepository.popMin(QUEUE_B, any()) } returns listOf(QueuedUser(200L, 200.0))
            every { queueRepository.issueToken(QUEUE_B, 200L, any(), any()) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert: 큐 B는 정상 처리되어야 함
            verify { queueRepository.popMin(QUEUE_A, any()) }
            verify { queueRepository.popMin(QUEUE_B, any()) }
            verify { queueRepository.issueToken(QUEUE_B, 200L, any(), any()) }
        }

        @Test
        fun `큐 A 토큰 발급 실패해도 큐 B 토큰은 정상 발급된다`() {
            // arrange
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(
                WaitingQueueRegistry.QueueConfig(name = QUEUE_A, throughputPerServerPerSecond = 175, activeTokenTTLSeconds = 300),
                WaitingQueueRegistry.QueueConfig(name = QUEUE_B, throughputPerServerPerSecond = 100, activeTokenTTLSeconds = 300),
            )
            every { queueRepository.popMin(QUEUE_A, any()) } returns listOf(QueuedUser(100L, 100.0))
            every { queueRepository.issueToken(QUEUE_A, 100L, any(), any()) } throws RuntimeException("큐 A 토큰 발급 오류")
            every { queueRepository.popMin(QUEUE_B, any()) } returns listOf(QueuedUser(200L, 200.0))
            every { queueRepository.issueToken(QUEUE_B, 200L, any(), any()) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert: 큐 B 토큰은 정상 발급되어야 함
            verify { queueRepository.issueToken(QUEUE_A, 100L, any(), any()) }
            verify { queueRepository.issueToken(QUEUE_B, 200L, any(), any()) }
        }
    }
}
