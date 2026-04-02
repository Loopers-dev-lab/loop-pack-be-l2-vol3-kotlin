package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueStore
import com.loopers.support.error.CoreException
import com.loopers.support.error.QueueErrorCode
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.data.redis.RedisConnectionFailureException

class GetQueuePositionUseCaseTest {

    private val orderQueueStore: OrderQueueStore = mockk()
    private val queueProperties = QueueProperties(
        scheduler = QueueProperties.Scheduler(intervalMs = 100, batchSize = 18),
        token = QueueProperties.Token(ttlSeconds = 300),
        redis = QueueProperties.RedisKeys(),
        maxQueueSize = 50000,
    )
    private val useCase = GetQueuePositionUseCase(orderQueueStore, queueProperties)

    @DisplayName("순번 조회")
    @Nested
    inner class Execute {

        @DisplayName("토큰이 발급된 유저는 READY 상태를 반환한다")
        @Test
        fun ready() {
            every { orderQueueStore.hasToken(1L) } returns true

            val result = useCase.execute(1L)

            assertThat(result.status).isEqualTo(QueueStatus.READY)
        }

        @DisplayName("대기 중인 유저는 WAITING 상태와 순번, 예상 대기 시간, Polling 주기를 반환한다")
        @Test
        fun waiting() {
            every { orderQueueStore.hasToken(1L) } returns false
            every { orderQueueStore.getPosition(1L) } returns 50L

            val result = useCase.execute(1L)

            assertAll(
                { assertThat(result.status).isEqualTo(QueueStatus.WAITING) },
                { assertThat(result.position).isEqualTo(50L) },
                { assertThat(result.estimatedWaitSeconds).isGreaterThanOrEqualTo(0L) },
                { assertThat(result.suggestedPollIntervalMs).isEqualTo(1000L) },
            )
        }

        @DisplayName("대기열에 없는 유저는 NOT_IN_QUEUE 상태를 반환한다")
        @Test
        fun notInQueue() {
            every { orderQueueStore.hasToken(1L) } returns false
            every { orderQueueStore.getPosition(1L) } returns null

            val result = useCase.execute(1L)

            assertThat(result.status).isEqualTo(QueueStatus.NOT_IN_QUEUE)
        }

        @DisplayName("position 1~100이면 suggestedPollIntervalMs는 1000ms이다")
        @Test
        fun pollInterval1to100() {
            every { orderQueueStore.hasToken(1L) } returns false
            every { orderQueueStore.getPosition(1L) } returns 100L

            val result = useCase.execute(1L)

            assertThat(result.suggestedPollIntervalMs).isEqualTo(1000L)
        }

        @DisplayName("position 101~1000이면 suggestedPollIntervalMs는 3000ms이다")
        @Test
        fun pollInterval101to1000() {
            every { orderQueueStore.hasToken(1L) } returns false
            every { orderQueueStore.getPosition(1L) } returns 500L

            val result = useCase.execute(1L)

            assertThat(result.suggestedPollIntervalMs).isEqualTo(3000L)
        }

        @DisplayName("position 1001 이상이면 suggestedPollIntervalMs는 5000ms이다")
        @Test
        fun pollIntervalOver1000() {
            every { orderQueueStore.hasToken(1L) } returns false
            every { orderQueueStore.getPosition(1L) } returns 2000L

            val result = useCase.execute(1L)

            assertThat(result.suggestedPollIntervalMs).isEqualTo(5000L)
        }

        @DisplayName("Redis 연결 실패 시 QUEUE_SERVICE_UNAVAILABLE 예외가 발생한다")
        @Test
        fun redisConnectionFailure() {
            every { orderQueueStore.hasToken(1L) } throws RedisConnectionFailureException("Connection refused")

            val exception = assertThrows<CoreException> {
                useCase.execute(1L)
            }
            assertThat(exception.errorCode).isEqualTo(QueueErrorCode.QUEUE_SERVICE_UNAVAILABLE)
        }
    }
}
