package com.loopers.infrastructure.payment.pg

import com.loopers.domain.payment.PaymentClient
import com.loopers.domain.payment.PaymentRequestResult
import com.loopers.domain.payment.PaymentStatusCheckResult
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.Duration

@Component
class LoopPaymentClient(
    private val webClient: WebClient,
    @Value("\${pg.base-url}") private val baseUrl: String,
    @Value("\${pg.loop.callback-url}") private val callbackUrl: String,
    @Value("\${pg.timeout.read-sec}") private val readTimeoutSec: Long,
) : PaymentClient {

    @CircuitBreaker(name = "loop-pg-payment", fallbackMethod = "paymentFallback")
    @Retry(name = "loop-pg-payment")
    override fun requestPayment(
        userId: Long,
        transactionId: String,
        orderId: Long,
        amount: BigDecimal,
        cardType: String,
        cardNo: String,
    ): PaymentRequestResult {
        val request = PgPaymentRequest(
            orderId = orderId.toString(),
            cardType = cardType,
            cardNo = cardNo,
            amount = amount.toPlainString(),
            callbackUrl = callbackUrl,
        )
        return performRequest(userId, request)
    }

    fun paymentFallback(
        userId: Long,
        transactionId: String,
        orderId: Long,
        amount: BigDecimal,
        cardType: String,
        cardNo: String,
        ex: Exception,
    ): PaymentRequestResult {
        throw CoreException(ErrorType.INTERNAL_ERROR, "PG payment service is unavailable. Please try again later.")
    }

    /**
     * PG에서 결제 상태를 조회합니다. (복구용)
     */
    override fun checkPaymentStatus(orderId: Long): PaymentStatusCheckResult {
        val response = webClient.get()
            .uri("$baseUrl/api/v1/payments?orderId=$orderId")
            .retrieve()
            .bodyToMono(PgPaymentStatusResponse::class.java)
            .block(Duration.ofSeconds(readTimeoutSec))
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "Payment status check failed")

        return PaymentStatusCheckResult(
            transactionId = response.transactionId,
            status = response.status,
            amount = response.amount,
            reason = response.reason,
        )
    }

    private fun performRequest(userId: Long, request: PgPaymentRequest): PaymentRequestResult {
        val response = webClient.post()
            .uri("$baseUrl/api/v1/payments")
            .header("X-USER-ID", userId.toString())
            .header("Content-Type", "application/json")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PgPaymentResponse::class.java)
            .block(Duration.ofSeconds(readTimeoutSec))
            ?: throw CoreException(ErrorType.INTERNAL_ERROR, "PG payment request failed")

        return PaymentRequestResult(
            transactionKey = response.transactionKey,
            orderId = response.orderId,
            cardType = response.cardType,
            cardNo = response.cardNo,
            amount = response.amount,
            status = response.status,
            reason = response.reason,
        )
    }
}
