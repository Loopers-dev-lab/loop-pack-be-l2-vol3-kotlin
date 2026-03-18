package com.loopers.infrastructure.payment

import com.loopers.application.payment.PaymentGatewayPort
import com.loopers.application.payment.PgPaymentRequest
import com.loopers.application.payment.PgPaymentResponse
import com.loopers.application.payment.PgTransactionDetail
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class PgClientAdapter(
    private val pgRestClient: RestClient,
) : PaymentGatewayPort {

    private val log = LoggerFactory.getLogger(javaClass)

    @Retry(name = "pgRetry", fallbackMethod = "requestPaymentFallback")
    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "requestPaymentFallback")
    override fun requestPayment(request: PgPaymentRequest): PgPaymentResponse {
        val response = pgRestClient.post()
            .uri("/api/v1/payments")
            .body(request)
            .retrieve()
            .body(PgPaymentResponse::class.java)

        return response ?: PgPaymentResponse(
            success = false,
            transactionKey = null,
            status = null,
            reason = "PG 응답이 비어있습니다.",
        )
    }

    @Suppress("unused")
    fun requestPaymentFallback(request: PgPaymentRequest, throwable: Throwable): PgPaymentResponse {
        log.warn("PG 결제 요청 fallback 실행. orderId={}, error={}", request.orderId, throwable.message)
        return PgPaymentResponse(
            success = false,
            transactionKey = null,
            status = null,
            reason = "PG 시스템 연결 실패: ${throwable.message}",
        )
    }

    override fun getTransactionStatus(userId: String, transactionKey: String): PgTransactionDetail {
        val response = pgRestClient.get()
            .uri("/api/v1/payments/{transactionKey}?userId={userId}", transactionKey, userId)
            .retrieve()
            .body(PgTransactionDetail::class.java)

        return response ?: throw IllegalStateException("PG 거래 상태 조회 응답이 비어있습니다.")
    }
}
