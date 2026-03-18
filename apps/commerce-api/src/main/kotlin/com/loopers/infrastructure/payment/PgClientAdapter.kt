package com.loopers.infrastructure.payment

import com.loopers.application.payment.PaymentGatewayPort
import com.loopers.application.payment.PgPaymentRequest
import com.loopers.application.payment.PgPaymentResponse
import com.loopers.application.payment.PgTransactionDetail
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class PgClientAdapter(
    private val pgRestClient: RestClient,
) : PaymentGatewayPort {

    private val log = LoggerFactory.getLogger(javaClass)

    @CircuitBreaker(name = "pgCircuit", fallbackMethod = "requestPaymentFallback")
    @Retry(name = "pgRetry")
    override fun requestPayment(request: PgPaymentRequest): PgPaymentResponse {
        val pgBody = PgRequestBody(
            orderId = request.orderId,
            cardType = request.cardType,
            cardNo = request.cardNo,
            amount = request.amount,
            callbackUrl = request.callbackUrl,
        )

        val response = pgRestClient.post()
            .uri("/api/v1/payments")
            .header(HEADER_USER_ID, request.userId)
            .body(pgBody)
            .retrieve()
            .body(object : ParameterizedTypeReference<PgApiResponse<PgTransactionResponse>>() {})

        val data = response?.data
        return if (data != null) {
            PgPaymentResponse(
                success = true,
                transactionKey = data.transactionKey,
                status = data.status,
                reason = data.reason,
            )
        } else {
            PgPaymentResponse(
                success = false,
                transactionKey = null,
                status = null,
                reason = "PG 응답이 비어있습니다.",
            )
        }
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
            .uri("/api/v1/payments/{transactionKey}", transactionKey)
            .header(HEADER_USER_ID, userId)
            .retrieve()
            .body(object : ParameterizedTypeReference<PgApiResponse<PgTransactionDetailResponse>>() {})

        val data = response?.data
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "PG 거래 상태 조회 응답이 비어있습니다.")

        return PgTransactionDetail(
            transactionKey = data.transactionKey,
            status = data.status,
            reason = data.reason,
        )
    }

    companion object {
        private const val HEADER_USER_ID = "X-USER-ID"
    }
}

data class PgRequestBody(
    val orderId: String,
    val cardType: String,
    val cardNo: String,
    val amount: Long,
    val callbackUrl: String,
)

data class PgApiResponse<T>(
    val meta: PgMetadata?,
    val data: T?,
)

data class PgMetadata(
    val result: String?,
    val errorCode: String?,
    val message: String?,
)

data class PgTransactionResponse(
    val transactionKey: String,
    val status: String?,
    val reason: String?,
)

data class PgTransactionDetailResponse(
    val transactionKey: String,
    val orderId: String?,
    val cardType: String?,
    val cardNo: String?,
    val amount: Long?,
    val status: String,
    val reason: String?,
)
