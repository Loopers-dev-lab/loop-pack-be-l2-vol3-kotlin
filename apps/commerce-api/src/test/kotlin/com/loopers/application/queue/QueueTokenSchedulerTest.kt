package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueueTokenSchedulerTest {

    private val orderQueueStore: OrderQueueStore = mockk()
    private val queueProperties = QueueProperties(
        scheduler = QueueProperties.Scheduler(intervalMs = 100, batchSize = 18),
        token = QueueProperties.Token(ttlSeconds = 300),
        redis = QueueProperties.RedisKeys(),
        maxQueueSize = 50000,
        enabled = true,
    )
    private val scheduler = QueueTokenScheduler(orderQueueStore, queueProperties)

    @DisplayName("토큰 발급 스케줄러")
    @Nested
    inner class IssueTokens {

        @DisplayName("대기열에 유저가 있으면 batch-size만큼 토큰을 발급한다")
        @Test
        fun issueTokens() {
            every { orderQueueStore.getQueueSize() } returns 100L
            every { orderQueueStore.issueTokens(18, 300) } returns 18L

            scheduler.issueTokens()

            verify(exactly = 1) { orderQueueStore.issueTokens(18, 300) }
        }

        @DisplayName("대기열이 비어있으면 토큰 발급을 시도하지 않는다")
        @Test
        fun emptyQueue() {
            every { orderQueueStore.getQueueSize() } returns 0L

            scheduler.issueTokens()

            verify(exactly = 0) { orderQueueStore.issueTokens(any(), any()) }
        }

        @DisplayName("대기열에 batch-size보다 적은 유저가 있어도 정상 동작한다")
        @Test
        fun lessThanBatchSize() {
            every { orderQueueStore.getQueueSize() } returns 5L
            every { orderQueueStore.issueTokens(18, 300) } returns 5L

            scheduler.issueTokens()

            verify(exactly = 1) { orderQueueStore.issueTokens(18, 300) }
        }

        @DisplayName("토큰 발급 중 예외가 발생해도 스케줄러가 중단되지 않는다")
        @Test
        fun exceptionHandled() {
            every { orderQueueStore.getQueueSize() } returns 100L
            every { orderQueueStore.issueTokens(18, 300) } throws RuntimeException("Redis error")

            scheduler.issueTokens()

            verify(exactly = 1) { orderQueueStore.issueTokens(18, 300) }
        }

        @DisplayName("enabled가 false이면 토큰 발급을 시도하지 않는다")
        @Test
        fun disabled() {
            val disabledProperties = queueProperties.copy(enabled = false)
            val disabledScheduler = QueueTokenScheduler(orderQueueStore, disabledProperties)

            disabledScheduler.issueTokens()

            verify(exactly = 0) { orderQueueStore.getQueueSize() }
            verify(exactly = 0) { orderQueueStore.issueTokens(any(), any()) }
        }
    }
}
