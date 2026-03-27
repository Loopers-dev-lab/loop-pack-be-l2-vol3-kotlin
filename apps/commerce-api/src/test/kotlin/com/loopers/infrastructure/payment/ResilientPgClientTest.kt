package com.loopers.infrastructure.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.Executors

@DisplayName("ResilientPgClient")
class ResilientPgClientTest {
    companion object {
        private const val ORDER_ID = 1L
        private const val PAYMENT_AMOUNT = 50_000L
    }

    private fun createRequest(): PgPaymentRequest {
        return PgPaymentRequest(
            orderId = ORDER_ID,
            amount = PAYMENT_AMOUNT,
        )
    }

    private fun createProperties(
        timeout: Duration = Duration.ofMillis(50),
        slidingWindowSize: Int = 2,
        minimumNumberOfCalls: Int = 2,
        waitDurationInOpenState: Duration = Duration.ofSeconds(30),
    ): PaymentResilienceProperties {
        return PaymentResilienceProperties(
            timeoutDuration = timeout,
            circuitBreakerSlidingWindowSize = slidingWindowSize,
            circuitBreakerMinimumNumberOfCalls = minimumNumberOfCalls,
            circuitBreakerWaitDurationInOpenState = waitDurationInOpenState,
            circuitBreakerFailureRateThreshold = 50f,
            circuitBreakerPermittedNumberOfCallsInHalfOpenState = 1,
        )
    }

    @DisplayName("requestPayment")
    @Nested
    inner class RequestPayment {
        @Test
        @DisplayName("PG 응답이 타임아웃을 넘기면 fallback 응답을 반환한다")
        fun returnsDeferred_whenDelegateTimesOut() {
            val delegate: PgClient = mockk()
            every { delegate.requestPayment(createRequest()) } answers {
                Thread.sleep(200)
                PgPaymentResponse(
                    orderId = ORDER_ID,
                    amount = PAYMENT_AMOUNT,
                    transactionId = "pg-100",
                    status = PgPaymentStatus.APPROVED,
                )
            }

            val executor = Executors.newSingleThreadExecutor()
            try {
                val client = ResilientPgClient(
                    delegate = delegate,
                    properties = createProperties(timeout = Duration.ofMillis(20)),
                    executor = executor,
                )

                val result = client.requestPayment(createRequest())

                assertThat(result.status).isEqualTo(PgPaymentStatus.DEFERRED)
                assertThat(result.orderId).isEqualTo(ORDER_ID)
                assertThat(result.amount).isEqualTo(PAYMENT_AMOUNT)
            } finally {
                executor.shutdownNow()
            }
        }

        @Test
        @DisplayName("PG 내부 장애가 반복되면 circuit open 상태에서 fallback 응답을 반환한다")
        fun returnsDeferredWithoutCallingDelegate_whenCircuitIsOpen() {
            val delegate: PgClient = mockk()
            every { delegate.requestPayment(createRequest()) } throws CoreException(ErrorType.INTERNAL_ERROR, "PG 장애")

            val executor = Executors.newSingleThreadExecutor()
            try {
                val client = ResilientPgClient(
                    delegate = delegate,
                    properties = createProperties(),
                    executor = executor,
                )

                val first = client.requestPayment(createRequest())
                val second = client.requestPayment(createRequest())
                val third = client.requestPayment(createRequest())

                assertThat(first.status).isEqualTo(PgPaymentStatus.DEFERRED)
                assertThat(second.status).isEqualTo(PgPaymentStatus.DEFERRED)
                assertThat(third.status).isEqualTo(PgPaymentStatus.DEFERRED)
                verify(exactly = 2) { delegate.requestPayment(createRequest()) }
            } finally {
                executor.shutdownNow()
            }
        }

        @Test
        @DisplayName("PG 비즈니스 실패는 fallback 하지 않고 그대로 전파한다")
        fun rethrowsBusinessFailure_whenDeclinedByPg() {
            val delegate: PgClient = mockk()
            every { delegate.requestPayment(createRequest()) } throws CoreException(ErrorType.CONFLICT, "카드 승인에 실패했습니다.")

            val executor = Executors.newSingleThreadExecutor()
            try {
                val client = ResilientPgClient(
                    delegate = delegate,
                    properties = createProperties(),
                    executor = executor,
                )

                assertThatThrownBy {
                    client.requestPayment(createRequest())
                }
                    .isInstanceOf(CoreException::class.java)
                    .hasFieldOrPropertyWithValue("errorType", ErrorType.CONFLICT)
            } finally {
                executor.shutdownNow()
            }
        }
    }
}
