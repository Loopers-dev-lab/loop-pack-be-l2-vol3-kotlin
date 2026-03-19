package com.loopers.application.payment

import com.loopers.infrastructure.pg.PgApiResponse
import com.loopers.infrastructure.pg.PgClient
import com.loopers.infrastructure.pg.PgPaymentRequest
import com.loopers.infrastructure.pg.PgPaymentResponse
import com.loopers.infrastructure.pg.PgTransactionDetailResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import feign.RetryableException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.ConnectException
import java.net.SocketTimeoutException

@Component
class PgPaymentClient(
    private val pgClient: PgClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "pgRetry")
    fun requestPayment(userId: String, request: PgPaymentRequest): PgApiResponse<PgPaymentResponse> {
        return pgClient.requestPayment(userId, request)
    }

    fun requestPaymentFallback(userId: String, request: PgPaymentRequest, t: Throwable): PgApiResponse<PgPaymentResponse> {
        val failureType = when {
            t is CallNotPermittedException -> "CIRCUIT_OPEN"
            t is RetryableException && t.cause is SocketTimeoutException -> "TIMEOUT_EXHAUSTED"
            t is RetryableException && t.cause is ConnectException -> "CONNECTION_REFUSED"
            t is RetryableException -> "RETRYABLE_EXHAUSTED"
            else -> "UNKNOWN"
        }
        log.warn("PG 결제 요청 fallback: type=$failureType, orderId=${request.orderId}", t)
        throw CoreException(ErrorType.SERVICE_UNAVAILABLE, "PG 시스템 장애로 결제 요청에 실패했습니다. 잠시 후 다시 시도해주세요.")
    }

    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "getPaymentStatusFallback")
    fun getPaymentStatus(userId: String, transactionKey: String): PgApiResponse<PgTransactionDetailResponse> {
        return pgClient.getPaymentStatus(userId, transactionKey)
    }

    fun getPaymentStatusFallback(
        userId: String,
        transactionKey: String,
        t: Throwable,
    ): PgApiResponse<PgTransactionDetailResponse> {
        log.warn("PG 상태 조회 fallback: transactionKey=$transactionKey", t)
        return PgApiResponse(
            meta = PgApiResponse.PgMeta(result = "SUCCESS", errorCode = null, message = null),
            data = PgTransactionDetailResponse(
                transactionKey = transactionKey,
                orderId = "",
                cardType = "",
                cardNo = "",
                amount = 0,
                status = "PENDING",
                reason = null,
            ),
        )
    }
}
