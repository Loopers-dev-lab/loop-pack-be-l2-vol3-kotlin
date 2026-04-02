package com.loopers.application.queue

import com.loopers.domain.queue.OrderQueueStore
import com.loopers.interfaces.api.queue.EntryTokenInterceptor
import com.loopers.support.error.CoreException
import com.loopers.support.error.QueueErrorCode
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.redis.RedisConnectionFailureException

class QueueGracefulDegradationTest {

    private val orderQueueStore: OrderQueueStore = mockk()
    private val queueProperties = QueueProperties(
        scheduler = QueueProperties.Scheduler(intervalMs = 100, batchSize = 18),
        token = QueueProperties.Token(ttlSeconds = 300),
        redis = QueueProperties.RedisKeys(),
        maxQueueSize = 50000,
        enabled = true,
    )

    @DisplayName("Redis 장애 시 Graceful Degradation")
    @Nested
    inner class RedisFailure {

        @DisplayName("대기열 진입 시 Redis 장애가 발생하면 503 응답을 반환한다")
        @Test
        fun enterQueueReturns503() {
            every { orderQueueStore.hasToken(any()) } throws RedisConnectionFailureException("Connection refused")
            val useCase = EnterQueueUseCase(orderQueueStore, queueProperties)

            val exception = assertThrows<CoreException> {
                useCase.execute(1L)
            }
            assertThat(exception.errorCode).isEqualTo(QueueErrorCode.QUEUE_SERVICE_UNAVAILABLE)
        }

        @DisplayName("순번 조회 시 Redis 장애가 발생하면 503 응답을 반환한다")
        @Test
        fun getPositionReturns503() {
            every { orderQueueStore.hasToken(any()) } throws RedisConnectionFailureException("Connection refused")
            val useCase = GetQueuePositionUseCase(orderQueueStore, queueProperties)

            val exception = assertThrows<CoreException> {
                useCase.execute(1L)
            }
            assertThat(exception.errorCode).isEqualTo(QueueErrorCode.QUEUE_SERVICE_UNAVAILABLE)
        }

        @DisplayName("토큰 검증 시 Redis 장애가 발생하면 fail-open으로 통과시킨다")
        @Test
        fun tokenVerificationFailOpen() {
            val authenticateUserUseCase = mockk<com.loopers.application.user.AuthenticateUserUseCase>()
            every { authenticateUserUseCase.execute(any(), any()) } returns 1L
            every { orderQueueStore.hasToken(any()) } throws RedisConnectionFailureException("Connection refused")

            val interceptor = EntryTokenInterceptor(authenticateUserUseCase, orderQueueStore, queueProperties)
            val request = org.springframework.mock.web.MockHttpServletRequest().apply {
                method = "POST"
                addHeader(com.loopers.support.constant.AuthHeaders.User.LOGIN_ID, "testuser")
                addHeader(com.loopers.support.constant.AuthHeaders.User.LOGIN_PW, "password123")
            }

            val result = interceptor.preHandle(request, org.springframework.mock.web.MockHttpServletResponse(), Any())

            assertThat(result).isTrue()
        }

        @DisplayName("스케줄러 실행 중 Redis 장애가 발생해도 스케줄러가 중단되지 않는다")
        @Test
        fun schedulerContinuesOnFailure() {
            every { orderQueueStore.getQueueSize() } returns 100L
            every { orderQueueStore.issueTokens(any(), any()) } throws RedisConnectionFailureException("Connection refused")
            val scheduler = QueueTokenScheduler(orderQueueStore, queueProperties)

            scheduler.issueTokens()

            // 예외가 발생하지 않고 정상 종료되면 성공
        }
    }
}
