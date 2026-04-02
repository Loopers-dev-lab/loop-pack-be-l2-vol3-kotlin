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

            // 배치 크기 = 175 / 10 = 17
            // 첫 호출에서 17명 반환 (score와 함께)
            every { queueRepository.popMin(QUEUE_NAME, 17L) } returns
                (1L..17L).map { QueuedUser(it, it.toDouble()) }.toList()
            every { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert: 정확히 17명에게만 토큰 발급
            verify(exactly = 17) { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) }
        }

        @Test
        fun `배치 크기 이하의 요청만 처리되고 초과 요청은 대기한다`() {
            // arrange: 100명이 popMin 요청했지만 배치 크기는 17
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 175,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)

            // popMin(17)만 호출됨 (배치 크기 = 175 / 10 = 17)
            every { queueRepository.popMin(QUEUE_NAME, 17L) } returns (1L..17L).map { QueuedUser(it, it.toDouble()) }.toList()
            every { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert: 정확히 배치 크기 17로만 호출됨
            verify(exactly = 1) { queueRepository.popMin(QUEUE_NAME, 17L) }
            // 17L로만 호출 확인
            verify { queueRepository.popMin(QUEUE_NAME, 17L) }
        }

        @Test
        fun `낮은 처리량으로도 최소 1명 이상은 처리한다`() {
            // arrange: 매우 낮은 처리량 (5 TPS)
            // 5 / 10 = 0.5 → maxOf(1L, ...) = 1L
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME + "_low",
                throughputPerServerPerSecond = 5,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)

            every { queueRepository.popMin(QUEUE_NAME + "_low", 1L) } returns listOf(QueuedUser(1L, 1.0))
            every { queueRepository.issueToken(QUEUE_NAME + "_low", any(), any(), any()) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert: 최소 1명은 처리됨
            verify(exactly = 1) { queueRepository.popMin(QUEUE_NAME + "_low", 1L) }
        }

        @Test
        fun `여러 배치에서 누적 처리량이 정확하다`() {
            // arrange: 175 TPS → 100ms마다 17명씩 = 1초에 170명
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 175,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)

            every { queueRepository.popMin(QUEUE_NAME, 17L) } returns (1L..17L).map { QueuedUser(it, it.toDouble()) }.toList()
            every { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) } returns Unit

            // act: 스케줄러 10회 실행 (1초)
            repeat(10) {
                queueScheduler.processQueue()
            }

            // assert: 총 170명 처리 (17명 × 10회)
            verify(exactly = 170) { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) }
        }
    }
}
