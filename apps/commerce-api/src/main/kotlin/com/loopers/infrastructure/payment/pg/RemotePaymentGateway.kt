package com.loopers.infrastructure.payment.pg

import com.loopers.domain.payment.PgPaymentGateway
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.math.BigDecimal
import java.time.Duration

import org.springframework.beans.factory.annotation.Value

@Component
class RemotePaymentGateway(
    private val webClient: WebClient,
    @Value("\${pg.base-url:http://localhost:8083}") private val baseUrl: String,
) : PgPaymentGateway {
    private val log = LoggerFactory.getLogger(javaClass)

    data class PgPaymentRequest(
        val orderId: String,
        val cardType: String,
        val cardNo: String,
        val amount: String,
        val callbackUrl: String,
    )

    data class PgPaymentResponse(
        val transactionKey: String,
        val orderId: String,
        val cardType: Any,
        val cardNo: String,
        val amount: Long,
        val status: Any,
        val reason: String?,
    )

    @CircuitBreaker(name = "pg-payment", fallbackMethod = "paymentFallback")
    @Retry(name = "pg-payment")
    override fun requestPayment(
        userId: Long,
        transactionId: String,
        orderId: Long,
        amount: BigDecimal,
        cardType: String,
        cardNo: String,
        callbackUrl: String,
    ): PgPaymentGateway.PaymentRequestResult {
        val request = PgPaymentRequest(
            orderId = orderId.toString(),
            cardType = cardType,
            cardNo = cardNo,
            amount = amount.toPlainString(),
            callbackUrl = callbackUrl,
        )

        log.info(
            "Requesting PG payment: orderId={}, transactionId={}, amount={}",
            orderId,
            transactionId,
            amount,
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
        callbackUrl: String,
        ex: Exception,
    ): PgPaymentGateway.PaymentRequestResult {
        log.error("Circuit breaker activated for PG payment: orderId={}, transactionId={}", orderId, transactionId, ex)
        throw RuntimeException("PG payment service is unavailable. Please try again later.", ex)
    }

    private fun performRequest(userId: Long, request: PgPaymentRequest): PgPaymentGateway.PaymentRequestResult {
        val response = webClient.post()
            .uri("$baseUrl/api/v1/payments")
            .header("X-USER-ID", userId.toString())
            .header("Content-Type", "application/json")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(PgPaymentResponse::class.java)
            .block(Duration.ofSeconds(10))
            ?: throw RuntimeException("PG payment request failed: no response")

        log.info(
            "Payment response: transactionKey={}, orderId={}, status={}",
            response.transactionKey,
            response.orderId,
            response.status,
        )

        return PgPaymentGateway.PaymentRequestResult(
            transactionKey = response.transactionKey,
            orderId = response.orderId,
            cardType = response.cardType,
            cardNo = response.cardNo,
            amount = response.amount,
            status = response.status,
            reason = response.reason,
        )
    }

    override fun verifySignature(transactionId: String, amount: BigDecimal, signature: String): Boolean {
        log.debug("Verifying signature for transactionId={}", transactionId)
        return true
    }
}
