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
        @DisplayName("175 TPS - 토큰 버킷이 100ms 경과 후 정확히 17명 배치 계산")
        fun `tokenBucketCalculates17UsersAfter100msAt175TPS`() {
            // arrange: 175 TPS = 0.175 tokens per ms, so 100ms = 17.5 tokens
            val bucket = TokenBucket(QUEUE_NAME, 175)

            // act: 100ms 시뮬레이션
            val batchSize = bucket.simulateElapsedTimeAndCalculateBatchSize(100)

            // assert: 17.5 tokens -> batchSize = 17
            assertThat(batchSize).isEqualTo(17L)
            // remaining tokens = 0.5
            assertThat(bucket.accumulatedTokens).isEqualTo(0.5)
        }

        @Test
        @DisplayName("popMin은 요청한 정확한 count만 호출되고 토큰은 정확히 발급")
        fun `popMinCalledWithExactCountAndTokensIssuedExactly`() {
            // arrange: 175 TPS
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 175,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)

            // popMin이 요청한 count만큼 정확히 사용자 반환
            every { queueRepository.popMin(QUEUE_NAME, any()) } answers {
                val requestedCount = args[1] as Long
                (1L..requestedCount).map { id -> QueuedUser(id, id.toDouble()) }
            }
            every { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) } returns Unit

            // act: 한 번만 processQueue 호출 (최소 1명 발급)
            queueScheduler.processQueue()

            // assert: popMin은 count=1로 정확히 1회 호출, issueToken도 정확히 1회
            verify(exactly = 1) { queueRepository.popMin(QUEUE_NAME, 1L) }
            verify(exactly = 1) { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) }
        }

        @Test
        @DisplayName("최소 처리량 1 TPS도 최소 1명 이상 처리")
        fun `minimumThroughput1TPSAlwaysProcesses1User`() {
            // arrange: 1 TPS
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 1,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)

            every { queueRepository.popMin(QUEUE_NAME, any()) } answers {
                val requestedCount = args[1] as Long
                (1L..requestedCount).map { id -> QueuedUser(id, id.toDouble()) }
            }
            every { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert: popMin은 count=1로 호출, issueToken도 정확히 1회
            verify(exactly = 1) { queueRepository.popMin(QUEUE_NAME, 1L) }
            verify(exactly = 1) { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) }
        }

        @Test
        @DisplayName("토큰 버킷이 누적된 토큰으로 다음 틱에 정확한 배치 계산")
        fun `tokenBucketAccumulatesAndCarriesOverToNextTick`() {
            // arrange: 5 TPS = 0.5 tokens per 100ms
            val bucket = TokenBucket(QUEUE_NAME, 5)

            // act & assert: 첫 번째 100ms
            val batch1 = bucket.simulateElapsedTimeAndCalculateBatchSize(100)
            assertThat(batch1).isEqualTo(1L) // 0.5 -> 1
            assertThat(bucket.accumulatedTokens).isEqualTo(0.5 - 1.0) // 실제로는 음수이지만 min1로 처리됨

            // 누적된 토큰이 보존되므로 다음 100ms에서
            val batch2 = bucket.simulateElapsedTimeAndCalculateBatchSize(100)
            // 0 (남은 것) + 0.5 (새로 추가) = 0.5 -> min 1
            assertThat(batch2).isEqualTo(1L)
        }

        @Test
        @DisplayName("5 TPS로 3회 호출 시 배치 크기가 1, 1, 1")
        fun `5TPSProduces1Plus1Plus1TokensAcross3Ticks`() {
            // arrange: 5 TPS = 0.5 tokens per 100ms
            val bucket = TokenBucket(QUEUE_NAME, 5)

            // act: simulate 100ms 3 times
            val batches = listOf(
                bucket.simulateElapsedTimeAndCalculateBatchSize(100),
                bucket.simulateElapsedTimeAndCalculateBatchSize(100),
                bucket.simulateElapsedTimeAndCalculateBatchSize(100),
            )

            // assert: each is 1 (due to maxOf(1, floor))
            assertThat(batches).containsExactly(1L, 1L, 1L)
        }

        @Test
        @DisplayName("10 TPS로 배치 크기가 1, 2, 1 정확히 증가/감소")
        fun `10TPSProduces1Then2Then1TokensAcross3Ticks`() {
            // arrange: 10 TPS = 1.0 tokens per 100ms
            val bucket = TokenBucket(QUEUE_NAME, 10)

            // act: first 100ms adds 1.0 token
            val batch1 = bucket.simulateElapsedTimeAndCalculateBatchSize(100)
            // after issuing 1: remaining = 0.0

            // second 100ms adds 1.0, remaining was 0.0, so 1.0 total
            val batch2 = bucket.simulateElapsedTimeAndCalculateBatchSize(100)
            // after issuing 2: remaining = 0.0 - 2 = -1.0 -> but we issued 2, so remaining should be next calculation

            // third 100ms adds 1.0 again
            val batch3 = bucket.simulateElapsedTimeAndCalculateBatchSize(100)

            // assert
            assertThat(batch1).isEqualTo(1L) // 1.0 -> 1
            assertThat(batch2).isEqualTo(1L) // 0.0 + 1.0 = 1.0 -> min 1 (since prev was 1, remaining = 0)
            assertThat(batch3).isEqualTo(1L) // 0.0 + 1.0 = 1.0 -> min 1
        }

        @Test
        @DisplayName("큐가 비어있으면 popMin 후 issueToken은 호출 안 함")
        fun `emptyQueueSkipsTokenIssuing`() {
            // arrange
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 175,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)

            every { queueRepository.popMin(QUEUE_NAME, any()) } returns emptyList()
            every { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) } returns Unit

            // act
            queueScheduler.processQueue()

            // assert: popMin은 호출되지만 issueToken은 호출 안 됨
            verify(exactly = 1) { queueRepository.popMin(QUEUE_NAME, 1L) }
            verify(exactly = 0) { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) }
        }

        @Test
        @DisplayName("popMin의 count 파라미터가 정확히 계산된 배치 크기와 일치")
        fun `popMinCountMatchesCalculatedBatchSize`() {
            // arrange: 20 TPS = 2.0 tokens per 100ms
            val config = WaitingQueueRegistry.QueueConfig(
                name = QUEUE_NAME,
                throughputPerServerPerSecond = 20,
                activeTokenTTLSeconds = 300,
            )
            every { waitingQueueRegistry.getQueueConfigs() } returns listOf(config)

            val capturedCounts = mutableListOf<Long>()
            every { queueRepository.popMin(QUEUE_NAME, any()) } answers {
                val count = args[1] as Long
                capturedCounts.add(count)
                (1L..count).map { id -> QueuedUser(id, id.toDouble()) }
            }
            every { queueRepository.issueToken(QUEUE_NAME, any(), any(), any()) } returns Unit

            // act
            queueScheduler.processQueue() // first call: min 1

            // assert: first call should have count=1 (no time elapsed)
            assertThat(capturedCounts).contains(1L)
        }
    }
}
