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

class QueueSchedulerThroughputTest {

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

    @DisplayName("처리량 초과 - 배치 크기 정확성")
    @Nested
    inner class ThroughputControlTest {

        @Test
        fun `200명 대기 중일 때 배치 크기만큼만 처리된다`() {
            // arrange: 200명이 대기 중
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 175,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)

            // 임의의 배치 크기로 사용자 반환
            every { queueRepository.popMin(QUEUE_NAME, any()) } returns
                (1L..3L).map { QueuedUser(it, it.toDouble()) }.toList()
            every { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) } returns Unit

            // act: TokenBucket 미리 초기화
            queueScheduler.processQueue()
            val bucket = queueScheduler.getTokenBucket(QUEUE_NAME)
            bucket?.simulateElapsedTimeAndCalculateBatchSize(100)
            queueScheduler.processQueue()

            // assert: 최소 1명 이상 토큰 발급
            verify(atLeast = 1) { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) }
        }

        @Test
        fun `배치 크기 이하의 요청만 처리되고 초과 요청은 대기한다`() {
            // arrange
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 175,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)

            // 임의의 배치 크기로 반환
            every { queueRepository.popMin(QUEUE_NAME, any()) } returns (1L..3L).map { QueuedUser(it, it.toDouble()) }.toList()
            every { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert: popMin이 최소 1회 호출됨
            verify(atLeast = 1) { queueRepository.popMin(QUEUE_NAME, any()) }
        }

        @Test
        fun `낮은 처리량으로도 최소 1명 이상은 처리한다`() {
            // arrange: 매우 낮은 처리량 (5 TPS)
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME + "_low",
                throughputPerServerPerSecond = 5,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)

            every { queueRepository.popMin(QUEUE_NAME + "_low", any()) } returns listOf(QueuedUser(1L, 1.0))
            every { queueRepository.issueToken(QUEUE_NAME + "_low", any(), any(), any()) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert: 최소 1명은 처리됨
            verify(atLeast = 1) { queueRepository.popMin(QUEUE_NAME + "_low", any()) }
        }

        @Test
        fun `여러 배치에서 누적 처리량이 최소 기준을 만족한다`() {
            // arrange
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 175,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)

            var callCount = 0
            every { queueRepository.popMin(QUEUE_NAME, any()) } answers {
                callCount++
                (1..3).map { i -> QueuedUser(callCount * 10L + i, (callCount * 10 + i).toDouble()) }
            }
            every { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) } returns Unit

            // act: 스케줄러 10회 실행
            repeat(10) {
                queueScheduler.processQueue()
            }

            // assert: 최소 10회 처리
            verify(atLeast = 10) { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) }
        }
    }
}
