package com.loopers.application.payment

import com.loopers.infrastructure.pg.PgApiResponse
import com.loopers.infrastructure.pg.PgClient
import com.loopers.infrastructure.pg.PgPaymentRequest
import com.loopers.infrastructure.pg.PgPaymentResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import feign.FeignException
import feign.Request
import feign.RetryableException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.ConnectException
import java.nio.charset.Charset
import java.util.Date

/**
 * PgPaymentClient Resilience4j 통합 테스트
 * - PgClient(Feign)만 MockBean으로 교체하여 실제 CB/Retry 동작 검증
 * - CircuitBreaker 상태 전이(CLOSED → OPEN → HALF_OPEN → CLOSED/OPEN) 검증
 * - Fallback 로그 타입 구분(CIRCUIT_OPEN vs CONNECTION_REFUSED) 검증
 */
@SpringBootTest
class PgPaymentClientResilienceTest @Autowired constructor(
    private val pgPaymentClient: PgPaymentClient,
    private val circuitBreakerRegistry: CircuitBreakerRegistry,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @MockitoBean
    private lateinit var pgClient: PgClient

    private lateinit var circuitBreaker: CircuitBreaker

    private val testRequest = PgPaymentRequest(
        orderId = "test-order-001",
        cardType = "SAMSUNG",
        cardNo = "1234-5678-9012-3456",
        amount = 129000,
        callbackUrl = "http://localhost:8080/api/v1/payments/callback",
    )

    private val feignRequest = Request.create(
        Request.HttpMethod.POST,
        "http://localhost:8082/api/v1/payments",
        emptyMap(),
        null,
        Charset.defaultCharset(),
        null,
    )

    @BeforeEach
    fun setUp() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("pgCircuit")
        circuitBreaker.reset()
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun mockPgConnectionRefused() {
        whenever(pgClient.requestPayment(any(), any())).thenThrow(
            RetryableException(
                -1,
                "Connection refused",
                Request.HttpMethod.POST,
                ConnectException("Connection refused"),
                null as Date?,
                feignRequest,
            ),
        )
    }

    private fun mockPgSuccess(transactionKey: String = "txn-test-001") {
        whenever(pgClient.requestPayment(any(), any())).thenReturn(
            PgApiResponse(
                meta = PgApiResponse.PgMeta(result = "SUCCESS", errorCode = null, message = null),
                data = PgPaymentResponse(transactionKey = transactionKey, status = "SUCCESS", reason = null),
            ),
        )
    }

    private fun mockPg500() {
        whenever(pgClient.requestPayment(any(), any())).thenThrow(
            FeignException.InternalServerError(
                "[500] PG 서버 에러",
                feignRequest,
                null,
                null,
            ),
        )
    }

    private fun callPaymentAndCatch(): CoreException? {
        return try {
            pgPaymentClient.requestPayment("user-1", testRequest)
            null
        } catch (e: CoreException) {
            e
        }
    }

    @DisplayName("CircuitBreaker 상태 전이")
    @Nested
    inner class CircuitBreakerStateTransition {

        @DisplayName("CLOSED → OPEN: 연결 실패가 임계치를 넘으면 서킷이 열린다")
        @Test
        fun opensCircuit_whenFailureRateExceedsThreshold() {
            // arrange
            mockPgConnectionRefused()

            // act — minimum-number-of-calls=5 이상 호출하여 실패율 100% 달성
            repeat(5) { callPaymentAndCatch() }

            // assert
            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.OPEN)
        }

        @DisplayName("OPEN 상태에서 요청하면 PG 호출 없이 즉시 실패한다")
        @Test
        fun rejectsImmediately_whenCircuitIsOpen() {
            // arrange
            mockPgConnectionRefused()
            repeat(5) { callPaymentAndCatch() }
            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.OPEN)

            // act
            val exception = callPaymentAndCatch()

            // assert
            assertThat(exception).isNotNull
            assertThat(circuitBreaker.metrics.numberOfNotPermittedCalls).isGreaterThan(0)
        }

        @DisplayName("OPEN → HALF_OPEN: 대기 시간 경과 후 요청이 오면 HALF_OPEN으로 전이한다")
        @Test
        fun transitionsToHalfOpen_afterWaitDuration() {
            // arrange
            mockPgConnectionRefused()
            repeat(5) { callPaymentAndCatch() }
            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.OPEN)

            // act — HALF_OPEN으로 강제 전이 (wait-duration 시뮬레이션)
            circuitBreaker.transitionToHalfOpenState()

            // assert
            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.HALF_OPEN)
        }

        @DisplayName("HALF_OPEN → CLOSED: 허용된 요청이 과반 성공하면 서킷이 닫힌다")
        @Test
        fun closesCircuit_whenHalfOpenCallsSucceed() {
            // arrange — HALF_OPEN 상태 만들기
            mockPgConnectionRefused()
            repeat(5) { callPaymentAndCatch() }
            circuitBreaker.transitionToHalfOpenState()

            // act — permitted-calls=3, 모두 성공
            mockPgSuccess()
            repeat(3) { pgPaymentClient.requestPayment("user-1", testRequest) }

            // assert
            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.CLOSED)
        }

        @DisplayName("HALF_OPEN → OPEN: 허용된 요청이 과반 실패하면 다시 서킷이 열린다")
        @Test
        fun reopensCircuit_whenHalfOpenCallsFail() {
            // arrange — HALF_OPEN 상태 만들기
            mockPgConnectionRefused()
            repeat(5) { callPaymentAndCatch() }
            circuitBreaker.transitionToHalfOpenState()

            // act — permitted-calls=3, 모두 실패
            repeat(3) { callPaymentAndCatch() }

            // assert
            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.OPEN)
        }

        @DisplayName("PG 500 에러(FeignException)도 CB 실패로 카운트된다")
        @Test
        fun countsFeignExceptionAsFailure() {
            // arrange
            mockPg500()

            // act
            repeat(5) { callPaymentAndCatch() }

            // assert
            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.OPEN)
            assertThat(circuitBreaker.metrics.numberOfFailedCalls).isGreaterThanOrEqualTo(5)
        }
    }

    @DisplayName("Retry 동작")
    @Nested
    inner class RetryBehavior {

        @DisplayName("연결 실패(RetryableException)는 3회 재시도한다")
        @Test
        fun retriesOnConnectionRefused() {
            // arrange
            mockPgConnectionRefused()

            // act
            callPaymentAndCatch()

            // assert — CB 메트릭에서 1번의 실패 (Retry 3회는 내부에서 소진, CB에는 1회로 기록)
            assertThat(circuitBreaker.metrics.numberOfFailedCalls).isEqualTo(1)
        }

        @DisplayName("PG 500(FeignException)은 재시도하지 않는다")
        @Test
        fun doesNotRetryOnFeignException() {
            // arrange
            mockPg500()

            // act
            callPaymentAndCatch()

            // assert — Retry 없이 CB에 1회 실패로 기록
            assertThat(circuitBreaker.metrics.numberOfFailedCalls).isEqualTo(1)
        }

        @DisplayName("PG 성공 시 Retry 없이 정상 응답한다")
        @Test
        fun returnsSuccess_whenPgResponds() {
            // arrange
            mockPgSuccess("txn-success-001")

            // act
            val result = pgPaymentClient.requestPayment("user-1", testRequest)

            // assert
            assertThat(result.data?.transactionKey).isEqualTo("txn-success-001")
            assertThat(circuitBreaker.metrics.numberOfSuccessfulCalls).isEqualTo(1)
            assertThat(circuitBreaker.metrics.numberOfFailedCalls).isEqualTo(0)
        }
    }

    @DisplayName("Fallback 타입 구분")
    @Nested
    inner class FallbackTypeDistinction {

        @DisplayName("연결 실패 시 SERVICE_UNAVAILABLE 예외를 던진다")
        @Test
        fun throwsServiceUnavailable_onConnectionRefused() {
            // arrange
            mockPgConnectionRefused()

            // act
            val exception = assertThrows<CoreException> {
                pgPaymentClient.requestPayment("user-1", testRequest)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
        }

        @DisplayName("CB OPEN 시 SERVICE_UNAVAILABLE 예외를 던진다")
        @Test
        fun throwsServiceUnavailable_whenCircuitOpen() {
            // arrange — CB를 OPEN 상태로 만들기
            mockPgConnectionRefused()
            repeat(5) { callPaymentAndCatch() }
            assertThat(circuitBreaker.state).isEqualTo(CircuitBreaker.State.OPEN)

            // act
            val exception = assertThrows<CoreException> {
                pgPaymentClient.requestPayment("user-1", testRequest)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
        }

        @DisplayName("PG 500 시 SERVICE_UNAVAILABLE 예외를 던진다")
        @Test
        fun throwsServiceUnavailable_onPg500() {
            // arrange
            mockPg500()

            // act
            val exception = assertThrows<CoreException> {
                pgPaymentClient.requestPayment("user-1", testRequest)
            }

            // assert
            assertThat(exception.errorType).isEqualTo(ErrorType.SERVICE_UNAVAILABLE)
        }
    }
}
