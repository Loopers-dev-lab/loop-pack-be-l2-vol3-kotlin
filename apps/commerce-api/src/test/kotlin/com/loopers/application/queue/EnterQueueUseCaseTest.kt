package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueStore
import com.loopers.support.error.CoreException
import com.loopers.support.error.QueueErrorCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.data.redis.RedisConnectionFailureException

class EnterQueueUseCaseTest {

    private val orderQueueStore: OrderQueueStore = mockk()
    private val queueProperties = QueueProperties(
        scheduler = QueueProperties.Scheduler(intervalMs = 100, batchSize = 18),
        token = QueueProperties.Token(ttlSeconds = 300),
        redis = QueueProperties.RedisKeys(),
        maxQueueSize = 50000,
    )
    private val useCase = EnterQueueUseCase(orderQueueStore, queueProperties)

    @DisplayName("대기열 진입")
    @Nested
    inner class Execute {

        @DisplayName("정상 진입하면 순번과 예상 대기 시간을 반환한다")
        @Test
        fun success() {
            every { orderQueueStore.hasToken(1L) } returns false
            every { orderQueueStore.getQueueSize() } returns 100L
            every { orderQueueStore.enqueue(1L, any()) } returns true
            every { orderQueueStore.getPosition(1L) } returns 100L

            val result = useCase.execute(1L)

            assertAll(
                { assertThat(result.position).isEqualTo(100L) },
                { assertThat(result.alreadyHasToken).isFalse() },
                { assertThat(result.estimatedWaitSeconds).isGreaterThanOrEqualTo(0L) },
            )
        }

        @DisplayName("이미 대기열에 있는 유저는 새로 넣지 않고 현재 순번을 반환한다")
        @Test
        fun duplicateEntry() {
            every { orderQueueStore.hasToken(1L) } returns false
            every { orderQueueStore.getQueueSize() } returns 100L
            every { orderQueueStore.enqueue(1L, any()) } returns false
            every { orderQueueStore.getPosition(1L) } returns 50L

            val result = useCase.execute(1L)

            assertAll(
                { assertThat(result.position).isEqualTo(50L) },
                { assertThat(result.alreadyHasToken).isFalse() },
            )
        }

        @DisplayName("이미 토큰을 보유한 유저는 대기열에 넣지 않고 즉시 입장 가능 응답을 반환한다")
        @Test
        fun alreadyHasToken() {
            every { orderQueueStore.hasToken(1L) } returns true

            val result = useCase.execute(1L)

            assertAll(
                { assertThat(result.position).isEqualTo(0L) },
                { assertThat(result.alreadyHasToken).isTrue() },
            )
            verify(exactly = 0) { orderQueueStore.enqueue(any(), any()) }
        }

        @DisplayName("대기열 최대 인원 초과 시 QUEUE_FULL 예외가 발생한다")
        @Test
        fun queueFull() {
            every { orderQueueStore.hasToken(1L) } returns false
            every { orderQueueStore.getQueueSize() } returns 50000L

            val exception = assertThrows<CoreException> {
                useCase.execute(1L)
            }
            assertThat(exception.errorCode).isEqualTo(QueueErrorCode.QUEUE_FULL)
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
