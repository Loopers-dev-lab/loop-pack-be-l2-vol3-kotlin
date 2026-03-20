package com.loopers.infrastructure.payment

import com.loopers.application.payment.PgClient
import com.loopers.application.payment.PgOrderPaymentsResponse
import com.loopers.application.payment.PgPaymentRequest
import com.loopers.application.payment.PgPaymentResponse
import com.loopers.application.payment.PgTransactionInfo
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class PgRestClient(
    private val pgRestTemplate: RestTemplate,
) : PgClient {
    companion object {
        private val logger = LoggerFactory.getLogger(PgRestClient::class.java)
        private const val USER_ID_HEADER = "X-USER-ID"
    }

    @CircuitBreaker(name = "pgPayment")
    override fun requestPayment(memberId: Long, request: PgPaymentRequest): PgPaymentResponse {
        val headers = HttpHeaders().apply { set(USER_ID_HEADER, memberId.toString()) }
        val entity = HttpEntity(request, headers)

        val response = pgRestTemplate.exchange(
            "/api/v1/payments",
            HttpMethod.POST,
            entity,
            object : ParameterizedTypeReference<PgApiResponse<PgPaymentResponse>>() {},
        )

        return response.body?.data
            ?: throw RuntimeException("PG 결제 요청 응답이 비어있습니다.")
    }

    @CircuitBreaker(name = "pgPayment")
    override fun getPaymentStatus(memberId: Long, transactionKey: String): PgTransactionInfo {
        val headers = HttpHeaders().apply { set(USER_ID_HEADER, memberId.toString()) }
        val entity = HttpEntity<Any>(headers)

        val response = pgRestTemplate.exchange(
            "/api/v1/payments/{transactionKey}",
            HttpMethod.GET,
            entity,
            object : ParameterizedTypeReference<PgApiResponse<PgTransactionInfo>>() {},
            transactionKey,
        )

        return response.body?.data
            ?: throw RuntimeException("PG 결제 상태 조회 응답이 비어있습니다.")
    }

    @CircuitBreaker(name = "pgPayment")
    override fun getPaymentsByOrderId(memberId: Long, orderId: String): PgOrderPaymentsResponse {
        val headers = HttpHeaders().apply { set(USER_ID_HEADER, memberId.toString()) }
        val entity = HttpEntity<Any>(headers)

        val response = pgRestTemplate.exchange(
            "/api/v1/payments?orderId={orderId}",
            HttpMethod.GET,
            entity,
            object : ParameterizedTypeReference<PgApiResponse<PgOrderPaymentsResponse>>() {},
            orderId,
        )

        return response.body?.data
            ?: throw RuntimeException("PG 주문 결제 조회 응답이 비어있습니다.")
    }
}

data class PgApiResponse<T>(
    val meta: PgMetadata,
    val data: T?,
)

data class PgMetadata(
    val result: String,
    val errorCode: String?,
    val message: String?,
)
