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
            every { queueRepository.popMin(QUEUE_NAME, 1L) } returns listOf(QueuedUser(1L, 1.0))
            every { queueRepository.issueToken(QUEUE_NAME, 1L, any(), 300L) } returns Unit

            // act: TokenBucket을 미리 초기화
            queueScheduler.processQueue()
            val bucket = queueScheduler.getTokenBucket(QUEUE_NAME)
            bucket?.simulateElapsedTimeAndCalculateBatchSize(100) // 충분한 토큰 누적
            queueScheduler.processQueue()

            // assert
            verify { queueRepository.issueToken(QUEUE_NAME, 1L, any(), 300L) }
        }
    }

    @DisplayName("배치 크기 계산 - 엣지 케이스")
    @Nested
    inner class BatchSizeCalculationTest {

        @Test
        fun `throughput이 5일 때 100ms 후 배치는 최소 1이다`() {
            val bucket = TokenBucket("test-5", tpsConfig = 5)
            val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(100)
            assertThat(batchSize).isEqualTo(1L) // maxOf(1L, 0.5) = 1
        }

        @Test
        fun `throughput이 175일 때 100ms 후 배치는 17 이상이다`() {
            val bucket = TokenBucket("test-175", tpsConfig = 175)
            val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(100)
            assertThat(batchSize).isGreaterThanOrEqualTo(17L) // maxOf(1L, 17.5) = 17
        }

        @Test
        fun `배치는 항상 최소 1 이상이다`() {
            val bucket = TokenBucket("test-minimal", tpsConfig = 1)
            repeat(10) {
                val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(10)
                assertThat(batchSize).isGreaterThanOrEqualTo(1L)
            }
        }
    }

    @DisplayName("토큰 발급 부분 실패 - Fail-Fast 검증")
    @Nested
    inner class PartialTokenIssueFailureTest {

        @Test
        fun `배치 중 일부 사용자만 토큰 발급 실패하면 실패한 것만 재삽입된다`() {
            // arrange
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 175,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)
            // 첫 번째 호출: 3명 반환
            every { queueRepository.popMin(QUEUE_NAME, any()) } returns
                listOf(QueuedUser(1L, 1.0), QueuedUser(2L, 2.0), QueuedUser(3L, 3.0))

            // 토큰 발급: 1번 성공, 2번 실패, 3번 성공
            every { queueRepository.issueToken(QUEUE_NAME, 1L, any(), any()) } returns Unit
            every { queueRepository.issueToken(QUEUE_NAME, 2L, any(), any()) } throws RuntimeException("Redis 오류")
            every { queueRepository.issueToken(QUEUE_NAME, 3L, any(), any()) } returns Unit

            // 재삽입: user-2만 다시 삽입
            every { queueRepository.enter(QUEUE_NAME, 2L, 2.0) } returns true

            // act: TokenBucket 미리 초기화
            queueScheduler.processQueue()
            val bucket = queueScheduler.getTokenBucket(QUEUE_NAME)
            bucket?.simulateElapsedTimeAndCalculateBatchSize(100)
            queueScheduler.processQueue()

            // assert: 모든 issueToken 호출 확인
            verify { queueRepository.issueToken(QUEUE_NAME, 1L, any(), any()) }
            verify { queueRepository.issueToken(QUEUE_NAME, 2L, any(), any()) }
            verify { queueRepository.issueToken(QUEUE_NAME, 3L, any(), any()) }

            // user-2만 재삽입 확인
            verify { queueRepository.enter(QUEUE_NAME, 2L, 2.0) }
        }
    }
}
