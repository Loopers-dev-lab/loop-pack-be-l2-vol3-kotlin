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

class QueueSchedulerRedisFailureTest {

    private lateinit var queueScheduler: QueueScheduler
    private lateinit var queueRepository: QueueRepository
    private lateinit var waitingQueueRegistry: WaitingQueueRegistry

    companion object {
        private const val QUEUE_A = "order-queue"
        private const val QUEUE_B = "payment-queue"
    }

    @BeforeEach
    fun setUp() {
        queueRepository = mockk()
        waitingQueueRegistry = mockk()
        queueScheduler = QueueScheduler(queueRepository, waitingQueueRegistry)
    }

    @DisplayName("Redis 장애 - Fail-Fast 격리")
    @Nested
    inner class RedisFailureIsolationTest {

        @Test
        fun `큐 A Redis popMin 실패해도 큐 B는 정상 처리된다`() {
            // arrange
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(
                WaitingQueueRegistry.QueueConfig(name = QUEUE_A, throughputPerServerPerSecond = 175, activeTokenTTLSeconds = 300),
                WaitingQueueRegistry.QueueConfig(name = QUEUE_B, throughputPerServerPerSecond = 100, activeTokenTTLSeconds = 300),
            )
            every { queueRepository.popMin(QUEUE_A, any()) } throws RuntimeException("Redis 연결 실패")
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
        fun `큐 A Redis issueToken 실패해도 큐 B는 정상 처리된다`() {
            // arrange
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(
                WaitingQueueRegistry.QueueConfig(name = QUEUE_A, throughputPerServerPerSecond = 175, activeTokenTTLSeconds = 300),
                WaitingQueueRegistry.QueueConfig(name = QUEUE_B, throughputPerServerPerSecond = 100, activeTokenTTLSeconds = 300),
            )
            every { queueRepository.popMin(QUEUE_A, any()) } returns listOf(QueuedUser(100L, 100.0))
            every { queueRepository.issueToken(QUEUE_A, 100L, any(), any()) } throws RuntimeException("Redis 연결 실패")
            every { queueRepository.popMin(QUEUE_B, any()) } returns listOf(QueuedUser(200L, 200.0))
            every { queueRepository.issueToken(QUEUE_B, 200L, any(), any()) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert: 큐 B는 정상 처리되어야 함
            verify { queueRepository.issueToken(QUEUE_A, 100L, any(), any()) }
            verify { queueRepository.issueToken(QUEUE_B, 200L, any(), any()) }
        }
    }
}
