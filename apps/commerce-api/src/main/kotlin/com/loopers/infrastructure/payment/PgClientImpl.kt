package com.loopers.infrastructure.payment

import com.loopers.domain.payment.PgClient
import com.loopers.domain.payment.PgPaymentRequest
import com.loopers.domain.payment.PgPaymentResponse
import com.loopers.domain.payment.PgPaymentStatusResponse
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class PgClientImpl(
    private val pgRestTemplate: RestTemplate,
) : PgClient {
    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "pgClient", fallbackMethod = "requestPaymentFallback")
    override fun requestPayment(request: PgPaymentRequest): PgPaymentResponse {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("X-USER-ID", request.userId.toString())
        }
        val httpEntity = HttpEntity(request, headers)
        val response = pgRestTemplate.postForEntity("/api/v1/payments", httpEntity, PgPaymentResponse::class.java)
        return response.body ?: throw CoreException(ErrorType.INTERNAL_ERROR, "PG 응답이 비어있습니다.")
    }

    @CircuitBreaker(name = "pgClient", fallbackMethod = "getPaymentStatusFallback")
    override fun getPaymentStatus(userId: Long, transactionKey: String): PgPaymentStatusResponse {
        val headers = HttpHeaders().apply {
            set("X-USER-ID", userId.toString())
        }
        val httpEntity = HttpEntity<Void>(headers)
        val response = pgRestTemplate.exchange(
            "/api/v1/payments/{transactionKey}",
            HttpMethod.GET,
            httpEntity,
            PgPaymentStatusResponse::class.java,
            transactionKey,
        )
        return response.body ?: throw CoreException(ErrorType.INTERNAL_ERROR, "PG 응답이 비어있습니다.")
    }

    @Suppress("unused")
    private fun requestPaymentFallback(request: PgPaymentRequest, e: Exception): PgPaymentResponse {
        log.warn("PG 결제 요청 실패 (CircuitBreaker fallback): orderId={}, error={}", request.orderId, e.message)
        throw CoreException(ErrorType.INTERNAL_ERROR, "PG 서버가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.")
    }

    @Suppress("unused")
    private fun getPaymentStatusFallback(userId: Long, transactionKey: String, e: Exception): PgPaymentStatusResponse {
        log.warn("PG 상태 조회 실패 (CircuitBreaker fallback): transactionKey={}, error={}", transactionKey, e.message)
        throw CoreException(ErrorType.INTERNAL_ERROR, "PG 서버가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.")
    }
}
